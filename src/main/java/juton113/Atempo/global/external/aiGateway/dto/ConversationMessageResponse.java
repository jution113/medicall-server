package juton113.Atempo.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ConversationMessageResponse {
    @JsonProperty("next_action")
    String nextAction;

    @JsonProperty("next_line_to_say")
    String conversationMessage;

    @JsonProperty("final_classification")
    String finalClassification;

}
