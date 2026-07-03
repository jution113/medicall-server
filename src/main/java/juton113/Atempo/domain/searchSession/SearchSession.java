package juton113.Atempo.domain.searchSession;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import juton113.Atempo.domain.incident.Incident;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "search_session")
public class SearchSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_session_id")
    private Long searchSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "search_radius", nullable = false)
    private Integer searchRadius;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private SearchSession(Incident incident,
                          Integer searchRadius) {
        this.incident = incident;
        this.searchRadius = searchRadius;
    }

    public static SearchSession from(Incident incident,
                                     Integer searchRadius) {
        return new SearchSession(incident, searchRadius);
    }
}
