package juton113.Medicall.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiPatientInfo {
    @JsonProperty("symptom")
    private String symptom;

    @JsonProperty("symptom_keyword")
    private String symptomKeyword;

    public static AiPatientInfo from(PatientInformation p) {
        return AiPatientInfo.builder()
                .symptom(p.getSymptom())
                .symptomKeyword(p.getSymptomKeyword().getDescription())
                .build();
    }
}