package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class RetrieveUserIdServiceImplTest {

    @Test
    void updateTransaction() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        Transaction2EnrichedMapper transaction2EnrichedMapper = Mockito.mock(Transaction2EnrichedMapper.class);

        String hpan = "HPAN1";
        String userId = "USERID1";
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        HpanInitiatives hpanInitiatives = HpanInitiatives.builder().hpan(hpan).userId(userId).build();
        TransactionEnrichedDTO transactionEnrichedDTO = Mockito.mock(TransactionEnrichedDTO.class);

        Mockito.when(hpanInitiativesRepository.findById(Mockito.anyString())).thenReturn(Mono.just(hpanInitiatives));

        Mockito.when(transaction2EnrichedMapper.apply(Mockito.same(transaction),Mockito.same(userId))).thenReturn(transactionEnrichedDTO);

        RetrieveUserIdService retrieveUserIdService = new RetrieveUserIdServiceImpl(hpanInitiativesRepository,transaction2EnrichedMapper);

        // When
        TransactionEnrichedDTO result = retrieveUserIdService.updateTransaction(transaction);

        // Then
        Assertions.assertEquals(transactionEnrichedDTO, result);
        Mockito.verify(hpanInitiativesRepository).findById(hpan);
        Mockito.verify(transaction2EnrichedMapper).apply(Mockito.same(transaction),Mockito.same(userId));


    }
}