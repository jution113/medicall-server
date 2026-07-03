package juton113.Atempo.domain.hospitalRanking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.reviewLog.enums.ReviewStatus;
import juton113.Atempo.domain.reviewLog.enums.ReviewTargetType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewInfo {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("review_target_type")
    private ReviewTargetType reviewTargetType;

    @JsonProperty("review_status")
    private ReviewStatus reviewStatus;

    @JsonProperty("comment")
    private String comment;

    public static ReviewInfo from(Long id,
                                  ReviewTargetType reviewTargetType,
                                  ReviewStatus reviewStatus,
                                  String comment) {
        return ReviewInfo.builder()
                .id(id)
                .reviewTargetType(reviewTargetType)
                .reviewStatus(reviewStatus)
                .comment(comment)
                .build();
    }
}
