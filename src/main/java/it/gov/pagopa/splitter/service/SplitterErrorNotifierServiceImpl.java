package it.gov.pagopa.splitter.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SplitterErrorNotifierServiceImpl implements SplitterErrorNotifierService {

    private final ErrorNotifierService errorNotifierService;

    private final String trxEnrichedMessagingServiceType;
    private final String trxEnrichedServer;
    private final String trxEnrichedTopic;
    private final String trxEnrichedGroup;

    private final String trxMessagingServiceType;
    private final String trxServer;
    private final String trxTopic;
    private final String trxGroup;

    @SuppressWarnings("squid:S00107") // suppressing too many parameters constructor alert
    public SplitterErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,

                                            @Value("${spring.cloud.stream.binders.kafka-idpay.type}") String trxEnrichedMessagingServiceType,
                                            @Value("${spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers}") String trxEnrichedServer,
                                            @Value("${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}") String trxEnrichedTopic,
                                            @Value("") String trxEnrichedGroup,

                                            @Value("${spring.cloud.stream.binders.kafka-rtd.type}") String trxMessagingServiceType,
                                            @Value("${spring.cloud.stream.binders.kafka-rtd.environment.spring.cloud.stream.kafka.binder.brokers}") String trxServer,
                                            @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}") String trxTopic,
                                            @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.group}") String trxGroup) {
        this.errorNotifierService = errorNotifierService;

        this.trxEnrichedMessagingServiceType = trxEnrichedMessagingServiceType;
        this.trxEnrichedServer = trxEnrichedServer;
        this.trxEnrichedTopic = trxEnrichedTopic;
        this.trxEnrichedGroup = trxEnrichedGroup;

        this.trxMessagingServiceType = trxMessagingServiceType;
        this.trxServer = trxServer;
        this.trxTopic = trxTopic;
        this.trxGroup = trxGroup;
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
    public void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(srcType, srcServer, srcTopic, group, message, description, retryable,resendApplication, exception);
    }
}
