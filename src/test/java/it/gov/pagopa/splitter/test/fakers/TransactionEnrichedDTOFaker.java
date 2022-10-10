package it.gov.pagopa.splitter.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionEnrichedDTOFaker {
    /** It will return an example of {@link TransactionEnrichedDTO}. Providing a bias, it will return a pseudo-casual object */

    public static TransactionEnrichedDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    /**
     * It will return an example of builder to obtain a {@link TransactionEnrichedDTO}. Providing a bias, it will return a pseudo-casual object
     */
    public static TransactionEnrichedDTO.TransactionEnrichedDTOBuilder<?, ?> mockInstanceBuilder(Integer bias){
        TransactionEnrichedDTO.TransactionEnrichedDTOBuilder<?, ?> out = TransactionEnrichedDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, TransactionDTOFaker.getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = TransactionDTOFaker.getFakeValuesService(bias);

        out.idTrxAcquirer("idTrxAcquirer_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.acquirerCode("acquirerCode_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.trxDate(OffsetDateTime.now());
        out.hpan("hpan_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.operationType("operationType_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.circuitType("circuitType_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.idTrxIssuer("idTrxIssuer_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.correlationId("correlationId_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.amount(new BigDecimal("100.00"));
        out.amountCurrency("amountCurrency_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.mcc("mcc_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.acquirerId("acquirerId_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.merchantId("merchantId_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.terminalId("terminalId_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.bin("bin_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.senderCode("senderCode_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.fiscalCode("fiscalCode_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.vat("vat_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.posType("posType_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.par("par_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.userId("userId_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.maskedPan("maskedPan_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.brandLogo("brandLogo_%d_%s".formatted(bias, fakeValuesService.bothify("???")));

        TestUtils.checkNotNullFields(out);
        return out;
    }
}
