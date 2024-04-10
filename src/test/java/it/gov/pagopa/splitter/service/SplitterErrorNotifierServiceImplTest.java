package it.gov.pagopa.splitter.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SplitterErrorNotifierServiceImplTest {
    private static final String BINDER_KAFKA_TYPE="kafka";
    private static final String BINDER_BROKER="broker";
    private static final String DUMMY_MESSAGE="DUMMY MESSAGE";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private SplitterErrorNotifierServiceImpl splitterErrorNotifierService;

    @BeforeEach
    void setUp() {
        splitterErrorNotifierService = new SplitterErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "trxProcessorOut-topic",
                "",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "trxProcessor-topic",
                "trxProcessor-group"
        );
    }

    @Test
    void notifyEnrichedTransaction() {
        errorNotifyMock("trxProcessorOut-topic","",true,false);
        splitterErrorNotifierService.notifyEnrichedTransaction(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void notifyTransactionEvaluation() {
        errorNotifyMock("trxProcessor-topic","trxProcessor-group",true,true);
        splitterErrorNotifierService.notifyTransactionEvaluation(dummyMessage,DUMMY_MESSAGE,true,new Throwable(DUMMY_MESSAGE));

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    @Test
    void testNotify() {
        errorNotifyMock("trxProcessor-topic","trxProcessor-group",true,true);
       splitterErrorNotifierService.notify(BINDER_KAFKA_TYPE,BINDER_BROKER,"trxProcessor-topic","trxProcessor-group",dummyMessage,DUMMY_MESSAGE,true,true,new Throwable(DUMMY_MESSAGE));

       Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(String topic, String group, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(BINDER_KAFKA_TYPE), eq(BINDER_BROKER),
                        eq(topic), eq(group), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}