package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapperTest;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
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
        Transaction2EnrichedMapper transaction2EnrichedMapper = Mockito.mock(Transaction2EnrichedMapper.class);
        SenderTransactionRejectedService senderTransactionRejectedService = Mockito.mock(SenderTransactionRejectedServiceImpl.class);
        RetrieveUserIdService retrieveUserIdService = new RetrieveUserIdServiceImpl(hpanInitiativesRepository,transaction2EnrichedMapper, senderTransactionRejectedService);

        String hpan = "HPAN_1";
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        transaction.setHpan(hpan);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives.setHpan(hpan);
        Mockito.when(hpanInitiativesRepository.findById(Mockito.anyString())).thenReturn(Mono.just(hpanInitiatives));

        TransactionEnrichedDTO transactionEnrichedDTO = new Transaction2EnrichedMapper().apply(transaction, hpanInitiatives);
        Mockito.when(transaction2EnrichedMapper.apply(Mockito.same(transaction),Mockito.same(hpanInitiatives))).thenReturn(transactionEnrichedDTO);

        // When
        TransactionEnrichedDTO result = retrieveUserIdService.resolveUserId(transaction).block();

        // Then
        Assertions.assertNotNull(result);
        Transaction2EnrichedMapperTest.assertionFromFieldOfTransactionDTO(transaction, result);
        Transaction2EnrichedMapperTest.assertionFromFieldOfHpanInitiatives(hpanInitiatives,result);
        Assertions.assertNotNull(result.getUserId());
        Mockito.verify(hpanInitiativesRepository).findById(hpan);
        Mockito.verify(transaction2EnrichedMapper).apply(Mockito.same(transaction), Mockito.same(hpanInitiatives));


    }

    @Test
    void updateTransactionHpanNotFound() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        Transaction2EnrichedMapper transaction2EnrichedMapper = Mockito.mock(Transaction2EnrichedMapper.class);
        SenderTransactionRejectedService senderTransactionRejectedService = Mockito.mock(SenderTransactionRejectedServiceImpl.class);
        RetrieveUserIdService retrieveUserIdService = new RetrieveUserIdServiceImpl(hpanInitiativesRepository,transaction2EnrichedMapper, senderTransactionRejectedService);

        String hpan = "HPAN_1";
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        transaction.setHpan(hpan);

        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives.setHpan(hpan);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        // When
        TransactionEnrichedDTO result = retrieveUserIdService.resolveUserId(transaction).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verify(hpanInitiativesRepository).findById(hpan);
        Mockito.verify(transaction2EnrichedMapper,Mockito.never()).apply(Mockito.same(transaction),Mockito.same(hpanInitiatives));
        Mockito.verify(senderTransactionRejectedService).sendTransactionRejected(Mockito.same(transaction));
    }
}