package it.gov.pagopa.splitter.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransactionEnrichedDTO extends TransactionDTO {
    private String userId;
}
