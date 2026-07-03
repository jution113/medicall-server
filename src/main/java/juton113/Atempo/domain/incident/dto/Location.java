package juton113.Atempo.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @JsonProperty("latitude")
    private BigDecimal latitude;

    @JsonProperty("longitude")
    private BigDecimal longitude;

    public static Location of(BigDecimal latitude, BigDecimal longitude) {
        return Location.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}
