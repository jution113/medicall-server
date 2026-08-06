package juton113.Medicall.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
public class AiBulkRequest {
    @JsonProperty("patient_info")
    private AiPatientInfo patientInfo;

    @JsonProperty("hospital_list")
    private List<AiHospitalInfo> hospitalList;
}