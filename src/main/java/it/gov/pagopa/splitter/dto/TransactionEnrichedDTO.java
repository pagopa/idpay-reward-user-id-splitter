package it.gov.pagopa.splitter.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class TransactionEnrichedDTO extends TransactionDTO {
    private String userId;
    private String maskedPan;
    private String brandLogo;
}
