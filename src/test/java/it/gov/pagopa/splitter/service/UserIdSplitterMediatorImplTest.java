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
import reactor.core.publisher.Flux;

@Slf4j
class UserIdSplitterMediatorImplTest {

    @Test
    void testExecute() {
        // Given
        RetrieveUserIdService retrieveUserIdService= Mockito.mock(RetrieveUserIdServiceImpl.class);
        MessageKeyedPreparation messageKeyedPreparation =Mockito.mock(MessageKeyedPreparationImpl.class);

        UserIdSplitterMediator mediator = new UserIdSplitterMediatorImpl(retrieveUserIdService, messageKeyedPreparation);

        TransactionDTO transactionDTO1 = TransactionDTOFaker.mockInstance(1);
        TransactionDTO transactionDTO2 = TransactionDTOFaker.mockInstance(2);
        Flux<TransactionDTO> transactionDTOFlux = Flux.just(transactionDTO1,transactionDTO2);

        TransactionEnrichedDTO transactionEnrichedDTO1 = new Transaction2EnrichedMapper().apply(transactionDTO1,"USERID1");
        TransactionEnrichedDTO transactionEnrichedDTO2 = new Transaction2EnrichedMapper().apply(transactionDTO2,"USERID2");
        Mockito.when(retrieveUserIdService.updateTransaction(transactionDTO1)).thenReturn(transactionEnrichedDTO1);
        Mockito.when(retrieveUserIdService.updateTransaction(transactionDTO2)).thenReturn(transactionEnrichedDTO2);

        Message message1 = Mockito.mock(Message.class);
        Message message2 = Mockito.mock(Message.class);
        Mockito.when(messageKeyedPreparation.apply(transactionEnrichedDTO1)).thenReturn(message1);
        Mockito.when(messageKeyedPreparation.apply(transactionEnrichedDTO2)).thenReturn(message2);


        // When
        Flux<Message<TransactionEnrichedDTO>> result = mediator.execute(transactionDTOFlux);

        // Then
        Assertions.assertEquals(2L,result.count().block());


    }
}