package juton113.Medicall.global.sse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.callArtifact.CallArtifact;
import juton113.Medicall.domain.incident.dto.Artifact;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResultEvent {
    @JsonProperty("hospital_ranking_id")
    private Long hospitalRankingId;

    @JsonProperty("artifact")
    private Artifact artifact;

    public static InquiryResultEvent from(Long hospitalRankingId,
                                          CallArtifact callArtifact) {
        return InquiryResultEvent.builder()
                .hospitalRankingId(hospitalRankingId)
                .artifact(Artifact.from(callArtifact))
                .build();
    }
}
