package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2RejectionMapper;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Sinks;

class TransactionRejectedSenderServiceImplTest {

    @Test
    void sendTransactionRejected() {
        // Given
        Sinks.Many<TransactionRejectedDTO> transactionRejectedDTOMany = Mockito.mock(Sinks.Many.class);
        Transaction2RejectionMapper transaction2RejectionMapper = Mockito.mock(Transaction2RejectionMapper.class);
        TransactionRejectedSenderServiceImpl transactionRejectedSenderService = new TransactionRejectedSenderServiceImpl(transactionRejectedDTOMany,transaction2RejectionMapper);

        TransactionDTO transactionDTO = TransactionDTOFaker.mockInstance(1);
        String rejectionReason = "REJECTION_FOR_HPAN_NOT_VALID";
        TransactionRejectedDTO transactionRejectedDTO = Mockito.mock(TransactionRejectedDTO.class);

        Mockito.when(transaction2RejectionMapper.apply(transactionDTO,rejectionReason)).thenReturn(transactionRejectedDTO);
        Mockito.doNothing().when(transactionRejectedDTOMany).emitNext(transactionRejectedDTO,Sinks.EmitFailureHandler.FAIL_FAST);


        // When
        transactionRejectedSenderService.sendTransactionRejected(transactionDTO);

        // Then
        Mockito.verify(transactionRejectedDTOMany).emitNext(Mockito.eq(transactionRejectedDTO),Mockito.any());
        Mockito.verify(transaction2RejectionMapper).apply(Mockito.any(),Mockito.eq(rejectionReason));
    }
}