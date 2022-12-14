package it.gov.pagopa.splitter.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate", "operationType", "acquirerId"}, callSuper = false)
public class TransactionDTO {
    String idTrxAcquirer;
    String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime trxDate;
    String hpan;
    String operationType;
    String circuitType;
    String idTrxIssuer;
    String correlationId;
    BigDecimal amount;
    String amountCurrency;
    String mcc;
    String acquirerId;
    String merchantId;
    String terminalId;
    String bin;
    String senderCode;
    String fiscalCode;
    String vat;
    String posType;
    String par;
}
