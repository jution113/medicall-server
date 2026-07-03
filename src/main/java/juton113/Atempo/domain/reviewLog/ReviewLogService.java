package juton113.Atempo.domain.reviewLog;

import jakarta.transaction.Transactional;
import juton113.Atempo.domain.reviewLog.dto.ReviewLogRequest;
import juton113.Atempo.domain.reviewLog.dto.ReviewStatusEvent;
import juton113.Atempo.domain.reviewLog.enums.ReviewTargetType;
import juton113.Atempo.domain.searchSession.SearchSession;
import juton113.Atempo.domain.searchSession.SearchSessionRepository;
import juton113.Atempo.global.exception.CustomException;
import juton113.Atempo.global.sse.SseService;
import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReviewLogService {
    private final SseService sseService;
    private final ReviewLogRepository reviewLogRepository;
    private final SearchSessionRepository searchSessionRepository;

    @Transactional
    public ReviewLog createReviewForRanking(Long sessionId, Long hospitalRankingId, ReviewLogRequest request) {
        SearchSession searchSession = searchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        ReviewLog reviewLog = ReviewLog.from(
                searchSession,
                ReviewTargetType.RANKING,
                hospitalRankingId,
                request.getReviewStatus(),
                request.getComment()
        );

        reviewLogRepository.save(reviewLog);

        sseService.sendReviewStatus(sessionId, ReviewStatusEvent.from(reviewLog));

        return reviewLog;
    }

    @Transactional
    public ReviewLog createReviewForInquiry(Long sessionId, Long inquiryId, ReviewLogRequest request) {
        SearchSession searchSession = searchSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        ReviewLog reviewLog = ReviewLog.from(
                searchSession,
                ReviewTargetType.INQUIRY,
                inquiryId,
                request.getReviewStatus(),
                request.getComment()
        );

        reviewLogRepository.save(reviewLog);

        sseService.sendReviewStatus(sessionId, ReviewStatusEvent.from(reviewLog));

        return reviewLog;
    }

    @Transactional
    public ReviewLog updateReviewLog(Long sessionId, Long reviewLogId, ReviewLogRequest request) {
        ReviewLog reviewLog = reviewLogRepository.findById(reviewLogId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        reviewLog.setReviewStatus(request.getReviewStatus());
        reviewLog.setComment(request.getComment());

        sseService.sendReviewStatus(sessionId, ReviewStatusEvent.from(reviewLog));

        return reviewLog;
    }

    public List<ReviewLog> getReviews(Long sessionId) {
        return reviewLogRepository.findBySearchSession_SearchSessionId(sessionId);
    }
}
