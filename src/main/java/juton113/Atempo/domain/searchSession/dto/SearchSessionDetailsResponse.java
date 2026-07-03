package juton113.Atempo.domain.searchSession.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.incident.dto.HospitalRankingInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSessionDetailsResponse {
    @JsonProperty("search_session_id")
    private Long searchSessionId;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("reviewed_count")
    private Integer reviewedCount;

    @JsonProperty("hospital_list")
    private List<HospitalRankingInfo> hospitalRankingInfos;

    public static SearchSessionDetailsResponse from(Long searchSessionId,
                                                    List<HospitalRankingInfo> hospitalRankingInfos) {
        return builder()
                .searchSessionId(searchSessionId)
                .totalCount(hospitalRankingInfos.size())
                .reviewedCount(0)
                .hospitalRankingInfos(hospitalRankingInfos)
                .build();
    }
}
