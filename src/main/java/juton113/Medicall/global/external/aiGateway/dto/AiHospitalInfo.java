package juton113.Medicall.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiHospitalInfo {
    @JsonProperty("hospital_code")
    private String hospitalCode;

    @JsonProperty("er_messages")
    private List<String> erMessages;

    @JsonProperty("unavailable_messages")
    private List<String> unavailableMessages;
}