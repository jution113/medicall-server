package juton113.Atempo.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.incident.Incident;
import juton113.Atempo.domain.incident.enums.Consciousness;
import juton113.Atempo.domain.incident.enums.Gender;
import juton113.Atempo.domain.incident.enums.KtasLevel;
import juton113.Atempo.domain.incident.enums.SymptomKeyword;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientInformation {
    @JsonProperty("ktas")
    private KtasLevel ktas;

    @JsonProperty("symptom")
    private String symptom;

    @JsonProperty("symptom_keyword")
    private SymptomKeyword symptomKeyword;

    @JsonProperty("age")
    private Integer age;

    @JsonProperty("gender")
    private Gender gender;

    @JsonProperty("consciousness")
    private Consciousness consciousness;

    @JsonProperty("vitals")
    private Vitals vitals;

    public static PatientInformation form(Incident incident) {
        return PatientInformation.builder()
                .ktas(incident.getPatientKtasLevel())
                .symptom(incident.getPatientSymptom())
                .symptomKeyword(incident.getPatientSymptomKeyword())
                .age(incident.getPatientAge())
                .gender(incident.getPatientGender())
                .consciousness(incident.getPatientConsciousness())
                .vitals(incident.getPatientVitals())
                .build();
    }

}
