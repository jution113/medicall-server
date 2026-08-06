package juton113.Medicall.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.member.enums.CertificationType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberCertificationInfoRequest {
    @JsonProperty("certification_type")
    CertificationType certificationType;

    @JsonProperty("certification_number")
    String certificationNumber;
}
