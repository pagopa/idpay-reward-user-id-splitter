package it.gov.pagopa.splitter.service;

import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    void notifyEnrichedTransaction(Message<?> message, String description, boolean retryable, Throwable exception);
    void notifyTransactionEvaluation(Message<?> message, String description, boolean retryable, Throwable exception);
    void notify(String srcType, String srcServer, String srcTopic, Message<?> message, String description, boolean retryable, Throwable exception);

}
