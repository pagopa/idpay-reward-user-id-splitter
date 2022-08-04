package it.gov.pagopa.splitter.dto.mapper;

import com.mongodb.assertions.Assertions;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import org.junit.jupiter.api.Test;

class Transaction2EnrichedWithoutUserIdMapperTest {

    @Test
    void apply() {
        // Given
        Transaction2EnrichedWithoutUserIdMapper transaction2EnrichedWithoutUserIdMapper = new Transaction2EnrichedWithoutUserIdMapper();
        TransactionDTO transactionDTO = TransactionDTOFaker.mockInstance(1);

        // When
        TransactionEnrichedDTO result = transaction2EnrichedWithoutUserIdMapper.apply(transactionDTO);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkTransactionEnrichedNotNullFields(result,"userId");
    }
}