package juton113.Medicall.domain.reviewLog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.reviewLog.ReviewLog;
import juton113.Medicall.domain.reviewLog.enums.ReviewStatus;
import juton113.Medicall.domain.reviewLog.enums.ReviewTargetType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatusEvent {
    @JsonProperty("review_target_type")
    private ReviewTargetType reviewTargetType;

    @JsonProperty("target_id")
    private Long targetId;

    @JsonProperty("review_status")
    private ReviewStatus reviewStatus;

    @JsonProperty("comment")
    private String comment;

    public static ReviewStatusEvent from(ReviewLog reviewLog) {
        return ReviewStatusEvent.builder()
                .reviewTargetType(reviewLog.getReviewTargetType())
                .targetId(reviewLog.getTargetId())
                .reviewStatus(reviewLog.getReviewStatus())
                .comment(reviewLog.getComment())
                .build();
    }
}
