package juton113.Atempo.global.external.aiGateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ConversationLog {
    @JsonProperty("role")
    String role;

    @JsonProperty("content")
    String content;

    @JsonProperty("timestamp")
    LocalDateTime timestamp;

    public static ConversationLog from(String role,
                                       String content) {
        return ConversationLog.builder()
                .role(role)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
