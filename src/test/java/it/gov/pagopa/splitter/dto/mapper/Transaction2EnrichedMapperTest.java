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
    void testApplyWithUserId() {
        // Given
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);
        String userId = "USERID";

        Transaction2EnrichedMapper transaction2EnrichedMapper = new Transaction2EnrichedMapper();
        // When
        TransactionEnrichedDTO result = transaction2EnrichedMapper.apply(transaction,userId);

        // Then
        assertionFromFieldOfTransactionDTO(transaction, result);
        Assertions.assertEquals(userId,result.getUserId());
        TestUtils.checkTransactionEnrichedNotNullFields(result);
    }
    @Test
    void testApplyWithUserIdNull() {
        // Given
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);

        Transaction2EnrichedMapper transaction2EnrichedMapper = new Transaction2EnrichedMapper();
        // When
        TransactionEnrichedDTO result = transaction2EnrichedMapper.apply(transaction,null);

        // Then
        assertionFromFieldOfTransactionDTO(transaction, result);
        Assertions.assertNull(result.getUserId());
        TestUtils.checkTransactionEnrichedNotNullFields(result,"userId");
    }

    private void assertionFromFieldOfTransactionDTO(TransactionDTO transaction, TransactionEnrichedDTO result) {
        Assertions.assertEquals(transaction.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertEquals(transaction.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(transaction.getTrxDate(), result.getTrxDate());
        Assertions.assertEquals(transaction.getHpan(), result.getHpan());
        Assertions.assertEquals(transaction.getOperationType(), result.getOperationType());
        Assertions.assertEquals(transaction.getCircuitType(), result.getCircuitType());
        Assertions.assertEquals(transaction.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertEquals(transaction.getCorrelationId(), result.getCorrelationId());
        Assertions.assertEquals(transaction.getAmount(), result.getAmount());
        Assertions.assertEquals(transaction.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertEquals(transaction.getMcc(), result.getMcc());
        Assertions.assertEquals(transaction.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(transaction.getMerchantId(), result.getMerchantId());
        Assertions.assertEquals(transaction.getTerminalId(), result.getTerminalId());
        Assertions.assertEquals(transaction.getBin(), result.getBin());
        Assertions.assertEquals(transaction.getSenderCode(), result.getSenderCode());
        Assertions.assertEquals(transaction.getFiscalCode(), result.getFiscalCode());
        Assertions.assertEquals(transaction.getVat(), result.getVat());
        Assertions.assertEquals(transaction.getPosType(), result.getPosType());
        Assertions.assertEquals(transaction.getPar(), result.getPar());
    }
}