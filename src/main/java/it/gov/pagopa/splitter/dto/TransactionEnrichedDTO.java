package it.gov.pagopa.splitter.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class TransactionEnrichedDTO extends TransactionDTO {
    @Builder.Default
    private String userId = null;
}
