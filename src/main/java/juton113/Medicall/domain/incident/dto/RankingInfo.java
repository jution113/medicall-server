package juton113.Medicall.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingInfo {
    @JsonIgnore
    private Long rankingId;

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("matching_capabilities")
    private List<String> matchingCapabilities;

    @JsonProperty("is_warning")
    private Boolean isWarning;

    @JsonProperty("warning_reason")
    private String warningReason;

    @JsonProperty("cooldown_applied_at")
    private String cooldownAppliedAt;

    @JsonProperty("cooldown_duration_seconds")
    private Long cooldownDurationSeconds;

    @JsonProperty("is_confirmed_for_inquiry")
    private Boolean isConfirmedForInquiry;

    public static RankingInfo from(
            HospitalRanking ranking,
            Double score,
            List<String> matchingCapabilities,
            Boolean isWarning,
            String warningReason,
            String cooldownAppliedAt,
            Long cooldownDurationSeconds,
            Boolean isConfirmedForInquiry) {

        return RankingInfo.builder()
                .rankingId(ranking.getHospitalRankingId())
                .rank(ranking.getRank())
                .score(score)
                .matchingCapabilities(matchingCapabilities)
                .isWarning(isWarning)
                .warningReason(warningReason)
                .cooldownAppliedAt(cooldownAppliedAt)
                .cooldownDurationSeconds(cooldownDurationSeconds)
                .isConfirmedForInquiry(isConfirmedForInquiry)
                .build();

    }
}
