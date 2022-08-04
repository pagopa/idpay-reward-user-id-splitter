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
        String rejectionReason = "REJECTION_FOR_HPAN_NOT_VALID";

        //When
        TransactionRejectedDTO result = transaction2RejectionMapper.apply(transactionDTO, rejectionReason);

        //Then
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getRejectionReasons().contains(rejectionReason));
        Assertions.assertEquals(List.of(rejectionReason),result.getRejectionReasons());
        TestUtils.checkTransactionRejectedNotNullFields(result, "userId");
    }
}