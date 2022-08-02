package it.gov.pagopa.splitter.dto;

import lombok.*;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionEnrichedDTO extends TransactionDTO {
    private String userId;
}
