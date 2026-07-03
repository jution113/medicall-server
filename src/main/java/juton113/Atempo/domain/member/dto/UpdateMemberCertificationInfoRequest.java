package juton113.Atempo.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.member.enums.CertificationType;
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
