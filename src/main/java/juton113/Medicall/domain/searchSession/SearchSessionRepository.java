package juton113.Medicall.domain.searchSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchSessionRepository extends JpaRepository<SearchSession, Long> {
}
