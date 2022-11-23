package it.gov.pagopa.splitter.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyEnrichedTransaction(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    @SuppressWarnings("squid:S00107") // suppressing too many parameters alert
    void notify(String srcType, String srcServer, String srcTopic, String group, Message<?> message, String description, boolean retryable,boolean resendApplication, Throwable exception);
}
