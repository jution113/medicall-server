package juton113.Medicall.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryMessageCreateRequest {
    @JsonProperty("patient_info")
    private PatientInformation patientInformation;

    @JsonProperty("on_site_treatment")
    private String onSiteTreatment;
}
