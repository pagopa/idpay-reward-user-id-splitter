package it.gov.pagopa.splitter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionEnrichedDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneId;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class UserIdSplitterMediatorImplTest {
    @Mock
    private RetrieveUserIdService retrieveUserIdService;

    @Mock
    private TransactionFilterService transactionFilterService;

    @Mock
    private TransactionNotifierService transactionNotifierService;

    @Mock
    private ErrorNotifierService errorNotifierService;

    private UserIdSplitterMediator userIdSplitterMediator;

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    @BeforeEach
    void setUp() {
        userIdSplitterMediator = new UserIdSplitterMediatorImpl(retrieveUserIdService,transactionFilterService,transactionNotifierService, errorNotifierService, 10000,TestUtils.objectMapper);
    }

    @Test
    void testExecute() {
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        TransactionDTO transactionDTO3 = TransactionDTOFaker.mockInstance(3);

        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2,transactionDTO3)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        Mockito.when(transactionFilterService.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO2)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO3)).thenReturn(true);

        TransactionEnrichedDTO trxEnrichedDTO1 = TransactionEnrichedDTOFaker.mockInstance(1);
        trxEnrichedDTO1.setHpan(transactionDTO1.getHpan());
        TransactionEnrichedDTO trxEnrichedDTO2 = TransactionEnrichedDTOFaker.mockInstance(2);
        trxEnrichedDTO2.setHpan(transactionDTO2.getHpan());

        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO1)).thenReturn(Mono.just(trxEnrichedDTO1));
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO2)).thenReturn(Mono.just(trxEnrichedDTO2));
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO3)).thenReturn(Mono.empty());

        Mockito.when(transactionNotifierService.notify(trxEnrichedDTO1)).thenReturn(true);
        Mockito.when(transactionNotifierService.notify(trxEnrichedDTO2)).thenReturn(false);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verify(transactionFilterService, Mockito.times(3)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdService, Mockito.times(3)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyEnrichedTransaction(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),Mockito.any(IllegalStateException.class));

    }

    @Test
    void testExecuteWithUserIdNull() {
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        Mockito.when(transactionFilterService.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO2)).thenReturn(true);

        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO1)).thenReturn(Mono.empty());
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO2)).thenReturn(Mono.empty());


        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(errorNotifierService);

        Mockito.verify(transactionFilterService, Mockito.times(2)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdService, Mockito.times(2)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(errorNotifierService, Mockito.never()).notifyEnrichedTransaction(Mockito.any(), Mockito.anyString(), Mockito.anyBoolean(),Mockito.any(IllegalStateException.class));
    }

    @Test
    void errorDeserializer(){
        // Given
        Flux<Message<String>> transactionDTOFlux = Flux.just("messageNotValid1", "messageNotValid2")
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(retrieveUserIdService);
        Mockito.verifyNoInteractions(transactionFilterService);
        Mockito.verifyNoInteractions(transactionNotifierService);

        Mockito.verify(errorNotifierService, Mockito.times(2)).notifyTransactionEvaluation(Mockito.any(Message.class), Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }

    @Test
    void errorGeneric(){
        // Given
        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        Flux<Message<String>> transactionDTOFlux = Flux.just(transactionDTO1)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);

        Mockito.when(transactionFilterService.filter(transactionDTO1)).thenReturn(true);

        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO1)).thenThrow(RuntimeException.class);

        // When
        userIdSplitterMediator.execute(transactionDTOFlux);

        // Then
        Mockito.verifyNoInteractions(transactionNotifierService);

        Mockito.verify(transactionFilterService, Mockito.times(1)).filter(Mockito.any(TransactionDTO.class));
        Mockito.verify(retrieveUserIdService, Mockito.times(1)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyTransactionEvaluation(Mockito.any(Message.class), Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(RuntimeException.class));
    }
}