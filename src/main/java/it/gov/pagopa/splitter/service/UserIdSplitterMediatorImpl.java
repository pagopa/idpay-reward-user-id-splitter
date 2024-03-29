package it.gov.pagopa.splitter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class UserIdSplitterMediatorImpl extends BaseKafkaConsumer<TransactionDTO, TransactionEnrichedDTO> implements UserIdSplitterMediator {
    private final RetrieveUserIdService retrieveUserIdService;
    private final TransactionFilterService transactionFilterService;
    private final TransactionNotifierService transactionNotifierService;
    private final SplitterErrorNotifierService splitterErrorNotifierService;
    private final Duration commitDelay;
    private final ObjectReader objectReader;

    public UserIdSplitterMediatorImpl(
            @Value("${spring.application.name}") String applicationName,
            RetrieveUserIdService retrieveUserIdService,
            TransactionFilterService transactionFilterService,
            TransactionNotifierService transactionNotifierService,
            SplitterErrorNotifierService splitterErrorNotifierService,

            @Value("${spring.cloud.stream.kafka.bindings.trxProcessor-in-0.consumer.ackTime}") long commitMillis,

            ObjectMapper objectMapper) {
        super(applicationName);
        this.retrieveUserIdService = retrieveUserIdService;
        this.transactionFilterService = transactionFilterService;
        this.transactionNotifierService = transactionNotifierService;
        this.splitterErrorNotifierService = splitterErrorNotifierService;
        this.commitDelay = Duration.ofMillis(commitMillis);

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<TransactionEnrichedDTO>> afterCommits2subscribe) {
        afterCommits2subscribe.subscribe(p -> log.info("[TRX_USERID_SPLITTER] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> splitterErrorNotifierService.notifyTransactionEvaluation(message, "[TRX_USERID_SPLITTER] Unexpected JSON", true, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        splitterErrorNotifierService.notifyTransactionEvaluation(message, "[TRX_USERID_SPLITTER] An error occurred evaluating transaction", true, e);
    }

    @Override
    protected Mono<TransactionEnrichedDTO> execute(TransactionDTO payload, Message<String> message, Map<String, Object> ctx) {
        return Mono.just(payload)
                .filter(this.transactionFilterService::filter)
                .flatMap(this.retrieveUserIdService::resolveUserId)

                .doOnNext(r -> {
                    try{
                        if(!transactionNotifierService.notify(r)){
                            throw new IllegalStateException("[TRX_USERID_SPLITTER] Something gone wrong while transaction notify");
                        }
                    } catch (Exception e){
                        log.error("[UNEXPECTED_TRX_PROCESSOR_ERROR] Unexpected error occurred publishing transaction: {}", r);
                        splitterErrorNotifierService.notifyEnrichedTransaction(TransactionNotifierServiceImpl.buildMessage(r), "[TRX_USERID_SPLITTER] An error occurred while publishing the transaction evaluation result", true, e);
                    }

                });
    }

    @Override
    public String getFlowName() {
        return "TRX_USERID_SPLITTER";
    }
}
