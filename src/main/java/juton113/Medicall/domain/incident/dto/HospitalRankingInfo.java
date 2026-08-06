package juton113.Medicall.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.hospitalRanking.dto.ReviewInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalRankingInfo {
    @JsonProperty("hospital_ranking_id")
    private Long hospitalRankingId;

    @JsonProperty("hospital_info")
    private HospitalInfo hospitalInfo;

    @JsonProperty("review_info_list")
    private List<ReviewInfo> reviewInfoList;

    @JsonProperty("ranking_info")
    private RankingInfo rankingInfo;

    @JsonProperty("inquiry_result")
    private InquiryResult inquiryResult;

    public static HospitalRankingInfo from(Long hospitalRankingId,
                                           HospitalInfo hospitalInfo,
                                           List<ReviewInfo> reviewInfoList,
                                           RankingInfo rankingInfo,
                                           InquiryResult inquiryResult) {
        return HospitalRankingInfo.builder()
                .hospitalRankingId(hospitalRankingId)
                .hospitalInfo(hospitalInfo)
                .reviewInfoList(reviewInfoList)
                .rankingInfo(rankingInfo)
                .inquiryResult(inquiryResult)
                .build();
    }
}
