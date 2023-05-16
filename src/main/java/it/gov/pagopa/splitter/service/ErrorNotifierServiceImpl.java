package it.gov.pagopa.splitter.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Service
@Slf4j
public class ErrorNotifierServiceImpl implements ErrorNotifierService {

    public static final String ERROR_MSG_HEADER_APPLICATION_NAME = "applicationName";
    public static final String ERROR_MSG_HEADER_GROUP = "group";
    public static final String ERROR_MSG_HEADER_SRC_TYPE = "srcType";
    public static final String ERROR_MSG_HEADER_SRC_SERVER = "srcServer";
    public static final String ERROR_MSG_HEADER_SRC_TOPIC = "srcTopic";
    public static final String ERROR_MSG_HEADER_DESCRIPTION = "description";
    public static final String ERROR_MSG_HEADER_RETRYABLE = "retryable";
    public static final String ERROR_MSG_HEADER_STACKTRACE = "stacktrace";

    private final StreamBridge streamBridge;
    private final String applicationName;

    private final String trxEnrichedMessagingServiceType;
    private final String trxEnrichedServer;
    private final String trxEnrichedTopic;
    private final String trxEnrichedGroup;

    private final String trxMessagingServiceType;
    private final String trxServer;
    private final String trxTopic;
    private final String trxGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public ErrorNotifierServiceImpl(StreamBridge streamBridge,
                                    @Value("${spring.application.name}") String applicationName,

                                    @Value("${spring.cloud.stream.binders.kafka-idpay.type}") String trxEnrichedMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers}") String trxEnrichedServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}") String trxEnrichedTopic,
                                    @Value("") String trxEnrichedGroup,

                                    @Value("${spring.cloud.stream.binders.kafka-rtd.type}") String trxMessagingServiceType,
                                    @Value("${spring.cloud.stream.binders.kafka-rtd.environment.spring.cloud.stream.kafka.binder.brokers}") String trxServer,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}") String trxTopic,
                                    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.group}") String trxGroup) {
        this.streamBridge = streamBridge;
        this.applicationName = applicationName;

        this.trxEnrichedMessagingServiceType = trxEnrichedMessagingServiceType;
        this.trxEnrichedServer = trxEnrichedServer;
        this.trxEnrichedTopic = trxEnrichedTopic;
        this.trxEnrichedGroup = trxEnrichedGroup;

        this.trxMessagingServiceType = trxMessagingServiceType;
        this.trxServer = trxServer;
        this.trxTopic = trxTopic;
        this.trxGroup = trxGroup;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class ErrorNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<Object>>> errors() {
            return Flux::empty;
        }
    }

    @Override
    public void notifyEnrichedTransaction(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(trxEnrichedMessagingServiceType, trxEnrichedServer, trxEnrichedTopic, trxEnrichedGroup, message, description, retryable, false, exception);
    }

    @Override
    public void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception) {
        notify(trxMessagingServiceType, trxServer, trxTopic, trxGroup, message, description, retryable,true, exception);
    }

    @Override
    public void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        log.info("[ERROR_NOTIFIER] notifying error: {}", description, exception);
        final MessageBuilder<?> errorMessage = MessageBuilder.fromMessage(message)
                .setHeader(ERROR_MSG_HEADER_SRC_TYPE, srcType)
                .setHeader(ERROR_MSG_HEADER_SRC_SERVER, srcServer)
                .setHeader(ERROR_MSG_HEADER_SRC_TOPIC, srcTopic)
                .setHeader(ERROR_MSG_HEADER_DESCRIPTION, description)
                .setHeader(ERROR_MSG_HEADER_RETRYABLE, retryable)
                .setHeader(ERROR_MSG_HEADER_STACKTRACE, ExceptionUtils.getStackTrace(exception));

        addExceptionInfo(errorMessage, "rootCause", ExceptionUtils.getRootCause(exception));
        addExceptionInfo(errorMessage, "cause", exception.getCause());

        byte[] receivedKey = message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, byte[].class);
        if(receivedKey!=null){
            errorMessage.setHeader(KafkaHeaders.MESSAGE_KEY, new String(receivedKey, StandardCharsets.UTF_8));
        }

        if (resendApplication){
            errorMessage.setHeader(ERROR_MSG_HEADER_APPLICATION_NAME, applicationName);
            errorMessage.setHeader(ERROR_MSG_HEADER_GROUP, group);
        }

        if (!streamBridge.send("errors-out-0", errorMessage.build())) {
            log.error("[ERROR_NOTIFIER] Something gone wrong while notifying error");
        }
    }

    private void addExceptionInfo(MessageBuilder<?> errorMessage, String exceptionHeaderPrefix, Throwable rootCause) {
        errorMessage
                .setHeader("%sClass".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getClass().getName() : null)
                .setHeader("%sMessage".formatted(exceptionHeaderPrefix), rootCause != null ? rootCause.getMessage() : null);
    }
}
