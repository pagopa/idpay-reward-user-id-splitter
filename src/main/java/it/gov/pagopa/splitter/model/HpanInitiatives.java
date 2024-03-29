package it.gov.pagopa.splitter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "hpan_initiatives_lookup")
public class HpanInitiatives {
    @Id
    private String hpan;
    private String maskedPan;
    private String brandLogo;
    private String brand;
    private String userId;
    private List<OnboardedInitiative> onboardedInitiatives;
}