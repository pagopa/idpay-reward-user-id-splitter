package it.gov.pagopa.splitter.service;

import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.splitter.utils.PerformanceLogger;
import it.gov.pagopa.splitter.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Base class to extend in order to configure a timed commit behavior when using KafkaBinder.
 * Other than extend this class, you should:
 * <ol>
 *     <li>Turn off the autoCommit (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.autoCommitOffset=false)</li>
 *     <li>Set the ackMode to MANUAL_IMMEDIATE (spring.cloud.stream.kafka.bindings.BINDINGNAME.consumer.ackMode=MANUAL_IMMEDIATE)</li>
 * </ol>
 * @param <T> The type of the message to read and deserialize
 * @param <R> The type of the message resulted
 */
@Slf4j
public abstract class BaseKafkaConsumer<T, R> {

    /** Key used inside the {@link Context} to store the startTime */
    protected static final String CONTEXT_KEY_START_TIME = "START_TIME";
    /** Key used inside the {@link Context} to store a msg identifier used for logging purpose */
    protected static final String CONTEXT_KEY_MSG_ID = "MSG_ID";

    private final String applicationName;

    private static final Collector<KafkaAcknowledgeResult<?>, ?, Map<Integer, Pair<Long, KafkaAcknowledgeResult<?>>>> kafkaAcknowledgeResultMapCollector =
            Collectors.groupingBy(KafkaAcknowledgeResult::partition
            , Collectors.teeing(
                    Collectors.minBy(Comparator.comparing(KafkaAcknowledgeResult::offset)),
                    Collectors.maxBy(Comparator.comparing(KafkaAcknowledgeResult::offset)),
                    (min, max) -> Pair.of(
                            min.map(KafkaAcknowledgeResult::offset).orElse(null),
                            max.orElse(null))
            ));

    protected BaseKafkaConsumer(String applicationName) {
        this.applicationName = applicationName;
    }

    record KafkaAcknowledgeResult<T> (Acknowledgment ack, Integer partition, Long offset, T result){
        public KafkaAcknowledgeResult(Message<?> message, T result) {
            this(
                    Utils.getHeaderValue(message, KafkaHeaders.ACKNOWLEDGMENT),
                    getMessagePartitionId(message),
                    getMessageOffset(message),
                    result
            );
        }
    }

    private static Integer getMessagePartitionId(Message<?> message) {
        return Utils.getHeaderValue(message, KafkaHeaders.RECEIVED_PARTITION_ID);
    }

    private static Long getMessageOffset(Message<?> message) {
        return Utils.getHeaderValue(message, KafkaHeaders.OFFSET);
    }

    /** It will ask the superclass to handle the messages, then sequentially it will acknowledge them */
    public final void execute(Flux<Message<String>> initiativeBeneficiaryRuleDTOFlux) {
        Flux<List<R>> processUntilCommits =
                initiativeBeneficiaryRuleDTOFlux
                        .flatMapSequential(this::executeAcknowledgeAware)

                        .buffer(getCommitDelay())
                        .map(p -> {
                            Map<Integer, Pair<Long, KafkaAcknowledgeResult<?>>> partition2Offsets = p.stream()
                                    .collect(kafkaAcknowledgeResultMapCollector);

                            log.info("[KAFKA_COMMIT][{}] Committing {} messages: {}", getFlowName(), p.size(),
                                    partition2Offsets.entrySet().stream()
                                            .map(e->"partition %d: %d - %d".formatted(e.getKey(),e.getValue().getKey(), e.getValue().getValue().offset()))
                                            .collect(Collectors.joining(";")));

                            partition2Offsets.forEach((partition, offsets) -> Optional.ofNullable(offsets.getValue().ack()).ifPresent(Acknowledgment::acknowledge));

                            return p.stream()
                                            .map(KafkaAcknowledgeResult::result)
                                            .filter(Objects::nonNull)
                                            .toList();
                                }
                        );

        subscribeAfterCommits(processUntilCommits);
    }

    /** The {@link Duration} to wait before to commit processed messages */
    protected abstract Duration getCommitDelay();

    /** {@link Flux} to which subscribe in order to start its execution and eventually perform some logic on results */
    protected abstract void subscribeAfterCommits(Flux<List<R>> afterCommits2subscribe);

    private Mono<KafkaAcknowledgeResult<R>> executeAcknowledgeAware(Message<String> message) {
        KafkaAcknowledgeResult<R> defaultAck = new KafkaAcknowledgeResult<>(message, null);

        byte[] retryingApplicationName = message.getHeaders().get(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, byte[].class);
        if(retryingApplicationName != null && !new String(retryingApplicationName, StandardCharsets.UTF_8).equals(this.applicationName)){
            log.info("[{}] Discarding message due to other application retry ({}): {}", getFlowName(), retryingApplicationName, message.getPayload());
            return Mono.just(defaultAck);
        }

        Map<String, Object> ctx=new HashMap<>();
        ctx.put(CONTEXT_KEY_START_TIME, System.currentTimeMillis());
        ctx.put(CONTEXT_KEY_MSG_ID, message.getPayload());

        return execute(message, ctx)
                .map(r -> new KafkaAcknowledgeResult<>(message, r))
                .defaultIfEmpty(defaultAck)

                .onErrorResume(e -> {
                    notifyError(message, e);
                    return Mono.just(defaultAck);
                })
                .doOnNext(r -> doFinally(message, r.result, ctx))
                ;
    }

    /** to perform some operation at the end of business logic execution, thus before to wait for commit. As default, it will perform an INFO logging with performance time */
    @SuppressWarnings({"sonar:S1172", "unused"}) // suppressing unused parameters
    protected void doFinally(Message<String> message, R r, Map<String, Object> ctx) {
        Long startTime = (Long)ctx.get(CONTEXT_KEY_START_TIME);
        String msgId = (String)ctx.get(CONTEXT_KEY_MSG_ID);
        if(startTime != null){
            PerformanceLogger.logTiming(getFlowName(), startTime,
                    "(partition: %s, offset: %s) %s".formatted(getMessagePartitionId(message), getMessageOffset(message), msgId));
        }
    }

    /** Name used for logging purpose */
    protected String getFlowName() {
        return getClass().getSimpleName();
    }

    /** It will deserialize the message and then call the {@link #execute(Object, Message, Map)} method */
    protected Mono<R> execute(Message<String> message, Map<String, Object> ctx){
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)
                .flatMap(payload->execute(payload, message, ctx));
    }

    /** The {@link ObjectReader} to use in order to deserialize the input message */
    protected abstract ObjectReader getObjectReader();
    /** The action to take if the deserialization will throw an error */
    protected abstract Consumer<Throwable> onDeserializationError(Message<String> message);
    /** The action to take if an unexpected exception occurs */
    protected abstract void notifyError(Message<String> message, Throwable e);

    /** The function invoked in order to process the current message */
    protected abstract Mono<R> execute(T payload, Message<String> message, Map<String, Object> ctx);

    /** It will read and deserialize {@link Message#getPayload()} using the given {@link #getObjectReader()} */
    protected T deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, getObjectReader(), onDeserializationError(message));
    }

}