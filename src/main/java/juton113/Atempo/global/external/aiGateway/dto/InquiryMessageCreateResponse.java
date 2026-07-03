package juton113.Atempo.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryMessageCreateResponse {
    @JsonProperty("first_monologue")
    private String firstMonologue;
}
