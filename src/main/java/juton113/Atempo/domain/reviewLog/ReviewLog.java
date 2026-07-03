package juton113.Atempo.domain.reviewLog;

import jakarta.persistence.*;
import juton113.Atempo.domain.reviewLog.enums.ReviewStatus;
import juton113.Atempo.domain.reviewLog.enums.ReviewTargetType;
import juton113.Atempo.domain.member.Member;
import juton113.Atempo.domain.searchSession.SearchSession;
import juton113.Atempo.global.auditing.BaseTimeEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "review_log")
public class ReviewLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SearchSession searchSession;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_target_type", nullable = false)
    private ReviewTargetType reviewTargetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false)
    private ReviewStatus reviewStatus;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    public static ReviewLog from(SearchSession searchSession,
                                 ReviewTargetType reviewTargetType,
                                 Long targetId,
                                 ReviewStatus reviewStatus,
                                 String comment) {
        return ReviewLog.builder()
                .searchSession(searchSession)
                .reviewTargetType(reviewTargetType)
                .targetId(targetId)
                .reviewStatus(reviewStatus)
                .comment(comment)
                .build();
    }
}
