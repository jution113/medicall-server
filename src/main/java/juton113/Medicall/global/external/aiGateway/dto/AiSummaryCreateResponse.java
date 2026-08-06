package juton113.Medicall.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiSummaryCreateResponse {
    @JsonProperty("ai_summary")
    private String aiSummary;
}
