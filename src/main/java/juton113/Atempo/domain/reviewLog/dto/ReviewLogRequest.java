package juton113.Atempo.domain.reviewLog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.reviewLog.enums.ReviewStatus;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ReviewLogRequest {
    @JsonProperty("review_status")
    ReviewStatus reviewStatus;

    @JsonProperty("comment")
    String comment;
}
