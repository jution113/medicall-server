package juton113.Medicall.domain.hospitalRanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryConfirmUpdateRequest {
    @JsonProperty("is_confirmed_for_inquiry")
    private boolean isConfirmedForInquiry;
}
