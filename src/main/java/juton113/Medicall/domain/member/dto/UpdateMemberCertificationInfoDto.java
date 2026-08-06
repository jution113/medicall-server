package juton113.Medicall.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.member.enums.CertificationType;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberCertificationInfoDto {
    Long memberId;

    @JsonProperty("certification_type")
    CertificationType certificationType;

    @JsonProperty("certification_number")
    String certificationNumber;
}
