package juton113.Atempo.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AiBulkResponse {
    @JsonProperty("hospital_list")
    private List<AiPenaltyInfo> hospitalList;
}