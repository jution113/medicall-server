package juton113.Medicall.domain.inquiryLog;

import io.lettuce.core.dynamic.annotation.Param;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InquiryLogRepository extends JpaRepository<InquiryLog, Long> {
    @Query("SELECT hr.hospital.code, COUNT(il) " +
            "FROM InquiryLog il " +
            "JOIN il.hospitalRanking hr " +
            "WHERE hr.hospital.code IN :hospitalCodes " +
            "AND il.callStartedAt >= :startTime " +
            "GROUP BY hr.hospital.code")
    List<Object[]> countGroupedByHospitalCodesAndCallStartedAtAfter(
            @Param("hospitalCodes") List<String> hospitalCodes,
            @Param("startTime") LocalDateTime startTime
    );

    InquiryLog findByHospitalRanking_hospitalRankingId(long hospitalRankingId);

    Optional<InquiryLog> findByCallId(String callId);

    List<InquiryLog> findByHospitalRankingIn(List<HospitalRanking> hospitalRankingList);
}
