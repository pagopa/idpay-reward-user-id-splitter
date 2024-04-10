package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionEnrichedDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class TransactionNotifierServiceImplTest {
    @Mock
    private StreamBridge streamBridgeMock;

    @InjectMocks
    private TransactionNotifierServiceImpl notifierService;


    @Test
    void testNotify() {
        //given
        TransactionEnrichedDTO transaction = TransactionEnrichedDTOFaker.mockInstance(1);

        when(streamBridgeMock.send(eq("trxProcessorOut-out-0"), any(Message.class))).thenReturn(true);

        //when
        boolean result = notifierService.notify(transaction);

        //then
        Mockito.verifyNoMoreInteractions(streamBridgeMock);
        assertTrue(result);

    }

    @Test
    void testTransactionNotifierProducerConfig(){
        TransactionNotifierServiceImpl.TransactionNotifierProducerConfig transactionNotifierProducerConfig = new TransactionNotifierServiceImpl.TransactionNotifierProducerConfig();

        Flux<Message<TransactionEnrichedDTO>> fluxMessage = transactionNotifierProducerConfig.trxProcessorOut().get();

        Assertions.assertEquals(Flux.empty(), fluxMessage);
    }
}