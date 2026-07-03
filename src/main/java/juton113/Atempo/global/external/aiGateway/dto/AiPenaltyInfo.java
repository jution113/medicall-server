package juton113.Atempo.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiPenaltyInfo {
    @JsonProperty("hospital_code")
    private String hospitalCode;

    @JsonProperty("rejection_probability")
    private double rejectionProbability;

    @JsonProperty("penalty_reason")
    private String penaltyReason;

    public static AiPenaltyInfo empty() {
        return new AiPenaltyInfo();
    }
}