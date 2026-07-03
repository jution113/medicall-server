package juton113.Atempo.domain.hospitalRanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreDetails {
    @JsonProperty("eta")
    private int eta;

    @JsonProperty("distance")
    private int distance;

    @JsonProperty("matching_capabilities")
    private List<String> matchingCapabilities;

    @JsonProperty("cooldown_appliedAt")
    private String cooldownAppliedAt;

    @JsonProperty("cooldown_duration_seconds")
    private Long cooldownDurationSeconds;

    public static ScoreDetails from(int eta,
                                    int distance,
                                    List<String> matchingCapabilities,
                                    String cooldownAppliedAt,
                                    Long cooldownDurationSeconds) {
        return ScoreDetails.builder()
                .eta(eta)
                .distance(distance)
                .matchingCapabilities(matchingCapabilities)
                .cooldownAppliedAt(cooldownAppliedAt)
                .cooldownDurationSeconds(cooldownDurationSeconds)
                .build();
    }
}
