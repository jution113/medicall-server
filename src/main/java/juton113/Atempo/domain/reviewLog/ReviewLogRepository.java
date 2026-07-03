package juton113.Atempo.domain.reviewLog;

import juton113.Atempo.domain.reviewLog.enums.ReviewTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewLogRepository  extends JpaRepository<ReviewLog, Long> {
    List<ReviewLog> findBySearchSession_SearchSessionId(Long sessionId);
}
