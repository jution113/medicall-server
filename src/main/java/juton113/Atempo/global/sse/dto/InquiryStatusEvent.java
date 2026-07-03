package juton113.Atempo.global.sse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.inquiryLog.enums.CallResponseStatus;
import juton113.Atempo.domain.inquiryLog.enums.CallStatus;
import juton113.Atempo.domain.inquiryLog.enums.RejectionReason;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryStatusEvent {
    @JsonProperty("hospital_ranking_id")
    private Long hospitalRankingId;

    @JsonProperty("call_status")
    private CallStatus callStatus;

    @JsonProperty("call_response_status")
    private CallResponseStatus callResponseStatus;

    @JsonProperty("rejection_reason_category")
    private RejectionReason rejectionReason;

    public static InquiryStatusEvent from(Long hospitalRankingId,
                                          CallStatus callStatus,
                                          CallResponseStatus callResponseStatus,
                                          RejectionReason rejectionReason) {
        return InquiryStatusEvent.builder()
                .hospitalRankingId(hospitalRankingId)
                .callStatus(callStatus)
                .callResponseStatus(callResponseStatus)
                .rejectionReason(rejectionReason)
                .build();
    }
}

