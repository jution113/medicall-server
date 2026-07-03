package juton113.Atempo.domain.incident;

import jakarta.transaction.Transactional;
import juton113.Atempo.domain.searchSession.dto.SearchSessionCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;

    @Transactional
    public Incident createIncident(SearchSessionCreateRequest request) {
        return incidentRepository.save(Incident.from(request));
    }

}
