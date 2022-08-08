package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
class UserIdSplitterMediatorImplTest {

    @Test
    void testExecute() {
        // Given
        RetrieveUserIdService retrieveUserIdService= Mockito.mock(RetrieveUserIdServiceImpl.class);
        MessageKeyedPreparation messageKeyedPreparation =Mockito.mock(MessageKeyedPreparationImpl.class);
        TransactionFilterService transactionFilterService = Mockito.mock(TransactionFilterServiceImpl.class);

        UserIdSplitterMediator mediator = new UserIdSplitterMediatorImpl(retrieveUserIdService, messageKeyedPreparation, transactionFilterService);

        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        TransactionDTO transactionDTO3 = TransactionDTOFaker.mockInstance(3);
        Flux<TransactionDTO> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2,transactionDTO3);

        Mockito.when(transactionFilterService.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO2)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO3)).thenReturn(true);

        TransactionEnrichedDTO transactionEnrichedDTO1 = new Transaction2EnrichedMapper().apply(transactionDTO1,"USERID1");
        TransactionEnrichedDTO transactionEnrichedDTO2 = new Transaction2EnrichedMapper().apply(transactionDTO2,"USERID2");
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO1)).thenReturn(Mono.just(transactionEnrichedDTO1));
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO2)).thenReturn(Mono.just(transactionEnrichedDTO2));
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO3)).thenReturn(Mono.empty());

        Message<TransactionEnrichedDTO> message1 = MessageBuilder.withPayload(new TransactionEnrichedDTO()).build();
        Message<TransactionEnrichedDTO> message2 = MessageBuilder.withPayload(new TransactionEnrichedDTO()).build();
        Mockito.when(messageKeyedPreparation.apply(transactionEnrichedDTO1)).thenReturn(message1);
        Mockito.when(messageKeyedPreparation.apply(transactionEnrichedDTO2)).thenReturn(message2);


        // When
        Flux<Message<TransactionEnrichedDTO>> result = mediator.execute(transactionDTOFlux);

        // Then
        Assertions.assertEquals(2L, result.count().block());
        Mockito.verify(retrieveUserIdService, Mockito.times(3)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(messageKeyedPreparation, Mockito.times(2)).apply(Mockito.any(TransactionEnrichedDTO.class));


    }

    @Test
    void testExecuteWithUserIdNull() {
        // Given
        RetrieveUserIdService retrieveUserIdService= Mockito.mock(RetrieveUserIdServiceImpl.class);
        MessageKeyedPreparation messageKeyedPreparation = Mockito.mock(MessageKeyedPreparationImpl.class);
        TransactionFilterService transactionFilterService = Mockito.mock(TransactionFilterServiceImpl.class);

        UserIdSplitterMediator mediator = new UserIdSplitterMediatorImpl(retrieveUserIdService, messageKeyedPreparation, transactionFilterService);

        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        Flux<TransactionDTO> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2);

        Mockito.when(transactionFilterService.filter(transactionDTO1)).thenReturn(true);
        Mockito.when(transactionFilterService.filter(transactionDTO2)).thenReturn(true);

        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO1)).thenReturn(Mono.empty());
        Mockito.when(retrieveUserIdService.resolveUserId(transactionDTO2)).thenReturn(Mono.empty());


        // When
        Flux<Message<TransactionEnrichedDTO>> result = mediator.execute(transactionDTOFlux);
        log.info(result.toString());

        // Then
        Assertions.assertEquals(0L, result.count().block());
        Mockito.verify(retrieveUserIdService, Mockito.times(2)).resolveUserId(Mockito.any(TransactionDTO.class));
        Mockito.verify(messageKeyedPreparation, Mockito.never()).apply(Mockito.any(TransactionEnrichedDTO.class));



    }
}