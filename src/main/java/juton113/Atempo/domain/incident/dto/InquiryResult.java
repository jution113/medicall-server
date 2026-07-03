package juton113.Atempo.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.inquiryLog.enums.CallResponseStatus;
import juton113.Atempo.domain.inquiryLog.enums.CallStatus;
import juton113.Atempo.domain.inquiryLog.enums.RejectionReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResult {
    @JsonProperty("id")
    private Long inquiryId;

    @JsonProperty("call_status")
    private CallStatus callStatus;

    @JsonProperty("call_response_status")
    private CallResponseStatus callResponseStatus;

    @JsonProperty("rejection_reason_keyword")
    private RejectionReason rejectionReasonKeyword;

    @JsonProperty("artifact")
    private Artifact artifact;

    public static InquiryResult from(Long inquiryId,
                                     CallStatus callStatus,
                                     CallResponseStatus callResponseStatus,
                                     RejectionReason rejectionReasonKeyword,
                                     Artifact artifact) {
        return InquiryResult.builder()
                .inquiryId(inquiryId)
                .callStatus(callStatus)
                .callResponseStatus(callResponseStatus)
                .rejectionReasonKeyword(rejectionReasonKeyword)
                .artifact(artifact)
                .build();
    }

}
