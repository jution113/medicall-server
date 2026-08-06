package juton113.Medicall.domain.callArtifact;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CallArtifactRepository extends MongoRepository<CallArtifact, String> {
    Optional<CallArtifact> findByInquiryId(Long inquiryId);
}