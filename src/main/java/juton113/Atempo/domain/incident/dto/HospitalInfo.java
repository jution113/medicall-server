package juton113.Atempo.domain.incident.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.hospital.Hospital;
import juton113.Atempo.global.external.dto.EtaResult;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalInfo {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("address")
    private String address;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("eta")
    private Integer eta;

    @JsonProperty("distance")
    private double distance;

    public static HospitalInfo from(Hospital hospital, int eta, int distance) {
        return HospitalInfo.builder()
                .id(hospital.getHospitalId())
                .name(hospital.getName())
                .address(hospital.getAddress())
                .phoneNumber(hospital.getPhoneNumber())
                .eta(eta)
                .distance(distance)
                .build();
    }

}
