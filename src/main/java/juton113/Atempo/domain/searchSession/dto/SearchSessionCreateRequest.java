package juton113.Atempo.domain.searchSession.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.incident.dto.Location;
import juton113.Atempo.domain.incident.dto.PatientInformation;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSessionCreateRequest {
    @JsonProperty("location")
    private Location location;

    @JsonProperty("search_radius")
    private int searchRadius;

    @JsonProperty("patient_info")
    private PatientInformation patientInformation;

    @JsonProperty("on_site_treatment")
    private String onSiteTreatment;

}
