package it.gov.pagopa.splitter.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TransactionRejectedDTO extends TransactionEnrichedDTO{
    private String status;

    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();

}
