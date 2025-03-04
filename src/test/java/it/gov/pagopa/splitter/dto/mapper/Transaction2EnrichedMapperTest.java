package it.gov.pagopa.splitter.dto.mapper;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.utils.RewardUserIdSplitterConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public
class Transaction2EnrichedMapperTest {

    @Test
    void testApplyWithUserId() {
        // Given
        TransactionDTO transaction = TransactionDTOFaker.mockInstance(1);

        Transaction2EnrichedMapper transaction2EnrichedMapper = new Transaction2EnrichedMapper();
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives.setHpan(transaction.getHpan());

        // When
        TransactionEnrichedDTO result = transaction2EnrichedMapper.apply(transaction,hpanInitiatives);

        // Then
        assertionFromFieldOfTransactionDTO(transaction, result);
        assertionFromFieldOfHpanInitiatives(hpanInitiatives, result);

    }

    public static void assertionFromFieldOfTransactionDTO(TransactionDTO transaction, TransactionEnrichedDTO result) {
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
        Assertions.assertEquals(transaction.getBusinessName(), result.getBusinessName());
    }

    public static void assertionFromFieldOfHpanInitiatives(HpanInitiatives hpanInitiatives, TransactionEnrichedDTO result) {
        Assertions.assertEquals(hpanInitiatives.getUserId(), result.getUserId());
        Assertions.assertEquals(hpanInitiatives.getMaskedPan(), result.getMaskedPan());
        Assertions.assertEquals(hpanInitiatives.getBrandLogo(), result.getBrandLogo());
        Assertions.assertEquals(hpanInitiatives.getBrand(), result.getBrand());
        Assertions.assertEquals(RewardUserIdSplitterConstants.TRX_CHANNEL_RTD, result.getChannel());
    }
}