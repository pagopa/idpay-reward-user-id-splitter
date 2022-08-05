package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2RejectionMapper;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.function.StreamBridge;

class SenderTransactionRejectedServiceImplTest {

    @Test
    void sendTransactionRejected() {
        // Given
        Transaction2RejectionMapper transaction2RejectionMapper = Mockito.mock(Transaction2RejectionMapper.class);
        StreamBridge streamBridge = Mockito.mock(StreamBridge.class);
        SenderTransactionRejectedServiceImpl transactionRejectedSenderService = new SenderTransactionRejectedServiceImpl(transaction2RejectionMapper, streamBridge);

        TransactionDTO transactionDTO = TransactionDTOFaker.mockInstance(1);
        String binding = "trxRejectedProducer-out-0";
        String rejectionReason = "INVALID_HPAN";
        TransactionRejectedDTO transactionRejectedDTO = Mockito.mock(TransactionRejectedDTO.class);

        Mockito.when(transaction2RejectionMapper.apply(transactionDTO,rejectionReason)).thenReturn(transactionRejectedDTO);
        Mockito.when(streamBridge.send(Mockito.eq(binding),Mockito.same(transactionRejectedDTO))).thenReturn(true);

        // When
        transactionRejectedSenderService.sendTransactionRejected(transactionDTO);

        // Then
        Mockito.verify(streamBridge).send(Mockito.eq(binding),Mockito.same(transactionRejectedDTO));
        Mockito.verify(transaction2RejectionMapper).apply(Mockito.any(),Mockito.eq(rejectionReason));
    }
}