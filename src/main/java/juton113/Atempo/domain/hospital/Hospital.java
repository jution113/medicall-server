package juton113.Atempo.domain.hospital;

import jakarta.persistence.*;
import juton113.Atempo.domain.hospital.converter.HospitalTypeCodeConverter;
import juton113.Atempo.domain.hospital.enums.HospitalTypeCode;
import juton113.Atempo.domain.incident.dto.Location;
import juton113.Atempo.global.auditing.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hospital")
public class Hospital extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hospital_id")
    private Long hospitalId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Convert(converter = HospitalTypeCodeConverter.class)
    @Column(name = "hospital_type_code", nullable = false)
    private HospitalTypeCode hospitalTypeCode;

    @Column(name = "address")
    private String address;

    @Embedded
    private Location location;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "api_data_generated_at")
    private LocalDateTime apiDataGeneratedAt;

    public void updateFromApi(String name, HospitalTypeCode typeCode, String address, Location location, String phoneNumber, LocalDateTime apiDataGeneratedAt) {
        this.name = name;
        this.hospitalTypeCode = typeCode;
        this.address = address;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.apiDataGeneratedAt = apiDataGeneratedAt;
    }
}
