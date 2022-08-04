package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedWithoutUserIdMapper;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class RetrieveUserIdServiceImplTest {

    @Test
    void updateTransactionHpanFound() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        Transaction2EnrichedMapper transaction2EnrichedMapper = new Transaction2EnrichedMapper();
        Transaction2EnrichedWithoutUserIdMapper transaction2EnrichedWithoutUserIdMapper = Mockito.mock(Transaction2EnrichedWithoutUserIdMapper.class);
        TransactionRejectedSenderService transactionRejectedSenderService = Mockito.mock(TransactionRejectedSenderServiceImpl.class);
        RetrieveUserIdService retrieveUserIdService = new RetrieveUserIdServiceImpl(hpanInitiativesRepository,transaction2EnrichedMapper, transactionRejectedSenderService);

        String hpan = "HPAN1";
        String userId = "USERID1";
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().hpan(hpan).userId(userId).build();

        Mockito.when(hpanInitiativesRepository.findById(Mockito.anyString())).thenReturn(Mono.just(hpanInitiatives));

        //Mockito.when(transaction2EnrichedMapper.apply(Mockito.same(transaction),Mockito.same(userId))).thenReturn(transactionEnrichedDTO);

        // When
        TransactionEnrichedDTO result = retrieveUserIdService.updateTransaction(transaction).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getUserId());
        Assertions.assertEquals(userId,result.getUserId());
        Mockito.verify(hpanInitiativesRepository).findById(hpan);
        Mockito.verify(transaction2EnrichedWithoutUserIdMapper,Mockito.never()).apply(Mockito.same(transaction));


    }

    @Test
    void updateTransactionHpanNotFound() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        Transaction2EnrichedMapper transaction2EnrichedMapper = Mockito.mock(Transaction2EnrichedMapper.class);
        TransactionRejectedSenderService transactionRejectedSenderService = Mockito.mock(TransactionRejectedSenderServiceImpl.class);
        RetrieveUserIdService retrieveUserIdService = new RetrieveUserIdServiceImpl(hpanInitiativesRepository,transaction2EnrichedMapper, transactionRejectedSenderService);

        String hpan = "HPAN1";
        String userId = "USERID1";
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        //Mockito.when(transaction2EnrichedMapper.apply(Mockito.same(transaction),Mockito.same(userId))).thenReturn(transactionEnrichedDTO);

        // When
        TransactionEnrichedDTO result = retrieveUserIdService.updateTransaction(transaction).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verify(hpanInitiativesRepository).findById(hpan);
        Mockito.verify(transaction2EnrichedMapper,Mockito.never()).apply(Mockito.same(transaction),Mockito.same(userId));
        Mockito.verify(transactionRejectedSenderService).sendTransactionRejected(Mockito.same(transaction));
    }
}