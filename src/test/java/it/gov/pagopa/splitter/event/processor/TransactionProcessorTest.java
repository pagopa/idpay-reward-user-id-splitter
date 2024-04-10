package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.service.UserIdSplitterMediator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private static UserIdSplitterMediator userIdSplitterMediator;

    private static TransactionProcessor transactionProcessor;

    @BeforeEach
    void setUp() {
        transactionProcessor = new TransactionProcessor(userIdSplitterMediator);
    }

    @Test
    void trxProcessor() {

        transactionProcessor.trxProcessor();
        Mockito.verifyNoMoreInteractions(userIdSplitterMediator);
    }
}