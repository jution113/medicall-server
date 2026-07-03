package juton113.Atempo.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vitals {
    @JsonProperty("bp_systolic")
    private Integer bpSystolic;

    @JsonProperty("bp_diastolic")
    private Integer bpDiastolic;

    @JsonProperty("heart_rate")
    private Integer heartRate;

    @JsonProperty("respiratory_rate")
    private Integer respiratoryRate;

    @JsonProperty("body_temperature")
    private Float bodyTemperature;

    @JsonProperty("spo2")
    private Integer spo2;

    @JsonProperty("blood_sugar_test")
    private Integer bloodSugarTest;

}
