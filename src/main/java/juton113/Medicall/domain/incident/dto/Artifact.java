package juton113.Medicall.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.callArtifact.CallArtifact;
import juton113.Medicall.global.external.aiGateway.dto.ConversationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Artifact {
    @JsonProperty("full_transcript")
    private List<ConversationLog> fullTranscript;

    @JsonProperty("recording_url")
    private String recordingUrl;

    @JsonProperty("ai_summary")
    private String aiSummary;

    public static Artifact from(CallArtifact callArtifact) {
        CallArtifact.ArtifactData data =  callArtifact.getArtifactData();

        return Artifact.builder()
                .fullTranscript(data.getFullTranscript())
                .recordingUrl(data.getRecordingUrl())
                .aiSummary(data.getAiSummary())
                .build();
    }

}
