package it.gov.pagopa.splitter.dto.mapper;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class Transaction2RejectionMapperTest {

    @Test
    void apply() {
        //Given
        Transaction2RejectionMapper transaction2RejectionMapper = new Transaction2RejectionMapper();
        TransactionDTO transactionDTO = TransactionDTOFaker.mockInstance(1);
        String rejectionReason = "INVALID_HPAN";

        //When
        TransactionRejectedDTO result = transaction2RejectionMapper.apply(transactionDTO, rejectionReason);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(transactionDTO.getIdTrxAcquirer(),result.getIdTrxAcquirer());
        Assertions.assertEquals(transactionDTO.getAcquirerCode(),result.getAcquirerCode());
        Assertions.assertEquals(transactionDTO.getTrxDate(),result.getTrxDate());
        Assertions.assertEquals(transactionDTO.getHpan(),result.getHpan());
        Assertions.assertEquals(transactionDTO.getOperationType(),result.getOperationType());
        Assertions.assertEquals(transactionDTO.getCircuitType(),result.getCircuitType());
        Assertions.assertEquals(transactionDTO.getIdTrxIssuer(),result.getIdTrxIssuer());
        Assertions.assertEquals(transactionDTO.getCorrelationId(),result.getCorrelationId());
        Assertions.assertEquals(transactionDTO.getAmount(),result.getAmount());
        Assertions.assertEquals(transactionDTO.getAmountCurrency(),result.getAmountCurrency());
        Assertions.assertEquals(transactionDTO.getMcc(),result.getMcc());
        Assertions.assertEquals(transactionDTO.getAcquirerId(),result.getAcquirerId());
        Assertions.assertEquals(transactionDTO.getMerchantId(),result.getMerchantId());
        Assertions.assertEquals(transactionDTO.getTerminalId(),result.getTerminalId());
        Assertions.assertEquals(transactionDTO.getBin(),result.getBin());
        Assertions.assertEquals(transactionDTO.getSenderCode(),result.getSenderCode());
        Assertions.assertEquals(transactionDTO.getFiscalCode(),result.getFiscalCode());
        Assertions.assertEquals(transactionDTO.getVat(),result.getVat());
        Assertions.assertEquals(transactionDTO.getPosType(),result.getPosType());
        Assertions.assertEquals(transactionDTO.getPar(),result.getPar());

        Assertions.assertNull(result.getUserId());
        Assertions.assertEquals("REJECTED", result.getStatus());
        Assertions.assertEquals(List.of(rejectionReason),result.getRejectionReasons());

        TestUtils.checkTransactionRejectedNotNullFields(result, "userId");
    }
}