package juton113.Atempo.domain.inquiryLog;

import juton113.Atempo.domain.reviewLog.ReviewLog;
import juton113.Atempo.domain.reviewLog.ReviewLogService;
import juton113.Atempo.domain.reviewLog.dto.ReviewLogRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/search-sessions/{sessionId}/inquiries")
public class InquiryLogController {
    private final ReviewLogService reviewLogService;

    @PostMapping("/{inquiryId}/review-logs")
    public ResponseEntity<Void> createInquiryReview(
            @PathVariable Long sessionId,
            @PathVariable Long inquiryId,
            @RequestBody ReviewLogRequest request) {

        ReviewLog newReview = reviewLogService.createReviewForInquiry(
                sessionId,
                inquiryId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{inquiryId}/review-logs/{reviewLogId}")
    public ResponseEntity<Void> updateInquiryReview(
            @PathVariable Long sessionId,
            @PathVariable Long inquiryId,
            @PathVariable Long reviewLogId,
            @RequestBody ReviewLogRequest request) {

        ReviewLog newReview = reviewLogService.updateReviewLog(
                sessionId,
                reviewLogId,
                request
        );

        return ResponseEntity.noContent().build();
    }
}
