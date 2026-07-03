package juton113.Atempo.domain.inquiryLog;

import jakarta.persistence.*;
import juton113.Atempo.domain.hospitalRanking.HospitalRanking;
import juton113.Atempo.domain.inquiryLog.enums.CallResponseStatus;
import juton113.Atempo.domain.inquiryLog.enums.CallStatus;
import juton113.Atempo.domain.inquiryLog.enums.RejectionReason;
import juton113.Atempo.global.auditing.BaseTimeEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inquiry_log")
public class InquiryLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_log_id")
    private Long inquiryLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_ranking_id", nullable = false)
    private HospitalRanking hospitalRanking;

    @Column(name = "call_id", nullable = false)
    private String callId;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false)
    private CallStatus callStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_response_status", nullable = false)
    private CallResponseStatus callResponseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason_category")
    private RejectionReason rejectionReasonCategory;

    @Column(name = "call_started_at", nullable = false)
    private LocalDateTime callStartedAt;

    @Column(name = "call_ended_at")
    private LocalDateTime callEndedAt;

    public static InquiryLog from(HospitalRanking hospitalRanking,
                                  String callId) {
        return builder()
                .hospitalRanking(hospitalRanking)
                .callId(callId)
                .callStatus(CallStatus.CALLED)
                .callResponseStatus(CallResponseStatus.NO_RESPONSE)
                .callStartedAt(LocalDateTime.now())
                .build();
    }
}
