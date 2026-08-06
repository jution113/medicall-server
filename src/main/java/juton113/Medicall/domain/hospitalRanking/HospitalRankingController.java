package juton113.Medicall.domain.hospitalRanking;

import juton113.Medicall.domain.hospitalRanking.dto.InquiryConfirmUpdateRequest;
import juton113.Medicall.domain.reviewLog.ReviewLog;
import juton113.Medicall.domain.reviewLog.ReviewLogService;
import juton113.Medicall.domain.reviewLog.dto.ReviewLogRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/search-sessions/{sessionId}/hospital-rankings")
public class HospitalRankingController {
    private final HospitalRankingService hospitalRankingService;
    private final ReviewLogService reviewLogService;

    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
    @PostMapping("/{hospitalRankingId}/review-logs")
    public ResponseEntity<Void> createRankingReview(
            @PathVariable Long sessionId,
            @PathVariable Long hospitalRankingId,
            @RequestBody ReviewLogRequest request) {

        ReviewLog newReview = reviewLogService.createReviewForRanking(
                sessionId,
                hospitalRankingId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
    @PatchMapping("/{hospitalRankingId}/review-logs/{reviewLogId}")
    public ResponseEntity<Void> updateRankingReview(
            @PathVariable Long sessionId,
            @PathVariable Long hospitalRankingId,
            @PathVariable Long reviewLogId,
            @RequestBody ReviewLogRequest request) {

        ReviewLog newReview = reviewLogService.updateReviewLog(
                sessionId,
                reviewLogId,
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
    @PatchMapping("/{hospitalRankingId}")
    public ResponseEntity<Void> updateInquiryConfirmStatus(
            @PathVariable Long sessionId,
            @PathVariable Long hospitalRankingId,
            @RequestBody InquiryConfirmUpdateRequest request) {
        hospitalRankingService.updateInquiryConfirmStatus(sessionId, hospitalRankingId, request);

        return ResponseEntity.noContent().build();
    }
}
