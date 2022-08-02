package it.gov.pagopa.splitter.dto.mapper;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class Transaction2EnrichedMapperTest {

    @Test
    void testApply() {
        // Given
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        String userId = "USERID";

        Transaction2EnrichedMapper transaction2EnrichedMapper = new Transaction2EnrichedMapper();
        // When
        TransactionEnrichedDTO result = transaction2EnrichedMapper.apply(transaction,userId);

        // Then
        Assertions.assertEquals(transaction.getHpan(),result.getHpan());
        Assertions.assertEquals(userId,result.getUserId());
        TestUtils.checkTransactionEnrichedNotNullFields(result);
    }
}