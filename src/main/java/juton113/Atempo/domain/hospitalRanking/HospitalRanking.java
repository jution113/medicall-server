package juton113.Atempo.domain.hospitalRanking;

import jakarta.persistence.*;
import juton113.Atempo.domain.hospital.Hospital;
import juton113.Atempo.domain.hospitalRanking.converter.ScoreDetailsConverter;
import juton113.Atempo.domain.hospitalRanking.dto.ScoreDetails;
import juton113.Atempo.domain.searchSession.SearchSession;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hospital_ranking")
public class HospitalRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hospital_ranking_id")
    private Long hospitalRankingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_session_id", nullable = false)
    private SearchSession searchSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "ranking")
    private Integer rank;

    @Column(name = "final_score")
    private Double finalScore;

    @Convert(converter = ScoreDetailsConverter.class)
    @Column(name = "score_details", columnDefinition = "TEXT")
    private ScoreDetails scoreDetails;

    @Column(name = "is_selected_by_ai")
    private Boolean isSelectedByAi;

    @Column(name = "deselection_reason")
    private String deselectionReason;

    @Column(name = "is_selected_by_user")
    private Boolean isSelectedByUser;

    public static HospitalRanking from(SearchSession searchSession,
                                       Hospital hospital,
                                       Double finalScore,
                                       ScoreDetails scoreDetails,
                                       String deselectionReason) {
        return HospitalRanking.builder()
                .searchSession(searchSession)
                .hospital(hospital)
                .finalScore(finalScore)
                .scoreDetails(scoreDetails)
                .isSelectedByAi(deselectionReason == null)
                .deselectionReason(deselectionReason)
                .isSelectedByUser(false)
                .build();
    }

}
