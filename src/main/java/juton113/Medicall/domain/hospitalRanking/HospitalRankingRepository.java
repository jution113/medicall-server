package juton113.Medicall.domain.hospitalRanking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRankingRepository extends JpaRepository<HospitalRanking, Long> {
    @EntityGraph(attributePaths = "hospital")
    List<HospitalRanking> findBySearchSession_searchSessionId(Long sessionId);

    @Override
    @EntityGraph(attributePaths = "hospital")
    List<HospitalRanking> findAllById(Iterable<Long> ids);
}
