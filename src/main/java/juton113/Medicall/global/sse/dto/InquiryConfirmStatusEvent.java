package juton113.Medicall.global.sse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryConfirmStatusEvent {
    @JsonProperty("hospital_ranking_id")
    private Long hospitalRankingId;

    @JsonProperty("is_confirmed_for_inquiry")
    private boolean isConfirmedForInquiry;

    public static InquiryConfirmStatusEvent from(Long hospitalRankingId,
                                          boolean isConfirmedForInquiry) {
        return InquiryConfirmStatusEvent.builder()
                .hospitalRankingId(hospitalRankingId)
                .isConfirmedForInquiry(isConfirmedForInquiry)
                .build();
    }
}

