package juton113.Medicall.domain.incident;

import jakarta.persistence.*;
import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.incident.converter.VitalsConverter;
import juton113.Medicall.domain.incident.dto.Location;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import juton113.Medicall.domain.incident.dto.Vitals;
import juton113.Medicall.domain.incident.enums.*;
import juton113.Medicall.domain.member.Member;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.global.auditing.BaseTimeEntity;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "incident")
public class Incident extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Long incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = true)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false)
    private IncidentStatus statusCode;

    @Embedded
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_ktas_level")
    private KtasLevel patientKtasLevel;

    @Column(name = "patient_symptom")
    private String patientSymptom;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_symptom_keyword")
    private SymptomKeyword patientSymptomKeyword;

    @Column(name = "patient_age")
    private Integer patientAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_gender")
    private Gender patientGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_consciousness")
    private Consciousness patientConsciousness;

    @Convert(converter = VitalsConverter.class)
    @Column(name = "patient_vitals", columnDefinition = "TEXT")
    private Vitals patientVitals; // JSON 문자열로 저장

    @Column(name = "on_site_treatment", columnDefinition = "TEXT")
    private String onSiteTreatment;

    public static Incident from(SearchSessionCreateRequest request) {
        PatientInformation info = request.getPatientInformation();

        return Incident.builder()
                .statusCode(IncidentStatus.SEARCHING)
                .location(request.getLocation())
                .patientKtasLevel(info.getKtas())
                .patientSymptom(info.getSymptom())
                .patientSymptomKeyword(info.getSymptomKeyword())
                .patientAge(info.getAge())
                .patientGender(info.getGender())
                .patientConsciousness(info.getConsciousness())
                .patientVitals(info.getVitals())
                .onSiteTreatment(request.getOnSiteTreatment())
                .build();
    }

}
