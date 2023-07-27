package it.gov.pagopa.splitter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionEnrichedDTOFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class UserIdSplitterMediatorImplTest {
    @Mock
    private RetrieveUserIdService retrieveUserIdServiceMock;

    @Mock
    private TransactionFilterService transactionFilterServiceMock;

    @Mock
    private TransactionNotifierService transactionNotifierServiceMock;

    @Mock
    private SplitterErrorNotifierService splitterErrorNotifierServiceMock;

    private UserIdSplitterMediator userIdSplitterMediator;

    @BeforeEach
    void setUp() {
        userIdSplitterMediator = new UserIdSplitterMediatorImpl("appName", retrieveUserIdServiceMock, transactionFilterServiceMock, transactionNotifierServiceMock, splitterErrorNotifierServiceMock, 10000,TestUtils.objectMapper);
    }

    @AfterEach
    void checkErrorInvocations(){
        Mockito.mockingDetails(splitterErrorNotifierServiceMock).getInvocations()
                .forEach(i-> System.out.println("Called errorNotifier: " + Arrays.toString(i.getArguments())));
    }

    @Test
    void testExecute() {
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        TransactionDTO transactionDTO3 = TransactionDTOFaker.mockInstance(3);

        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2,transactionDTO3)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        Mockito.when(transactionFilterServiceMock.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterServiceMock.filter(transactionDTO2)).thenReturn(true);
        Mockito.when(transactionFilterServiceMock.filter(transactionDTO3)).thenReturn(true);

        TransactionEnrichedDTO trxEnrichedDTO1 = TransactionEnrichedDTOFaker.mockInstance(1);
        trxEnrichedDTO1.setHpan(transactionDTO1.getHpan());
        TransactionEnrichedDTO trxEnrichedDTO2 = TransactionEnrichedDTOFaker.mockInstance(2);
        trxEnrichedDTO2.setHpan(transactionDTO2.getHpan());

        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO1)).thenReturn(Mono.just(trxEnrichedDTO1));
        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO2)).thenReturn(Mono.just(trxEnrichedDTO2));
        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO3)).thenReturn(Mono.empty());

        Mockito.when(transactionNotifierServiceMock.notify(trxEnrichedDTO1)).thenReturn(true);
        Mockito.when(transactionNotifierServiceMock.notify(trxEnrichedDTO2)).thenReturn(false);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verify(transactionFilterServiceMock, Mockito.times(3)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdServiceMock, Mockito.times(3)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(splitterErrorNotifierServiceMock, Mockito.times(1)).notifyEnrichedTransaction(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),Mockito.any(IllegalStateException.class));

    }

    @Test
    void testExecuteWithUserIdNull() {
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        Mockito.when(transactionFilterServiceMock.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterServiceMock.filter(transactionDTO2)).thenReturn(true);

        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO1)).thenReturn(Mono.empty());
        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO2)).thenReturn(Mono.empty());


        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(splitterErrorNotifierServiceMock);

        Mockito.verify(transactionFilterServiceMock, Mockito.times(2)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdServiceMock, Mockito.times(2)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(splitterErrorNotifierServiceMock, Mockito.never()).notifyEnrichedTransaction(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),Mockito.any(IllegalStateException.class));
    }

    @Test
    void errorDeserializer(){
        // Given
        Flux<Message<String>> transactionDTOFlux = Flux.just("messageNotValid1", "messageNotValid2")
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(retrieveUserIdServiceMock);
        Mockito.verifyNoInteractions(transactionFilterServiceMock);
        Mockito.verifyNoInteractions(transactionNotifierServiceMock);

        Mockito.verify(splitterErrorNotifierServiceMock, Mockito.times(2)).notifyTransactionEvaluation(Mockito.any(Message.class), Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }

    @Test
    void errorGeneric(){
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        Mockito.when(transactionFilterServiceMock.filter(transactionDTO1)).thenReturn(true);

        Mockito.when(retrieveUserIdServiceMock.resolveUserId(transactionDTO1)).thenThrow(RuntimeException.class);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(transactionNotifierServiceMock);

        Mockito.verify(transactionFilterServiceMock, Mockito.times(1)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdServiceMock, Mockito.times(1)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(splitterErrorNotifierServiceMock, Mockito.times(1)).notifyTransactionEvaluation(Mockito.any(Message.class), Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(RuntimeException.class));
    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        TransactionDTO trx1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO trx2 = TransactionDTOFaker.mockInstance(2);

        Flux<Message<String>> msgs = Flux.just(trx1, trx2)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .doOnNext(m->m.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        userIdSplitterMediator.execute(msgs);

        // Then
        Mockito.verifyNoInteractions(retrieveUserIdServiceMock,transactionFilterServiceMock,transactionNotifierServiceMock, splitterErrorNotifierServiceMock);
    }
}