package juton113.Medicall.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.member.enums.CertificationType;
import juton113.Medicall.global.auth.enums.Role;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMemberResponseDto {
    String name;

    @JsonProperty("nick_name")
    String nickName;

    String email;

    @JsonProperty("profile_url")
    String profileUrl;

    Role role;

    @JsonProperty("certification_type")
    CertificationType certificationType;

    @JsonProperty("certification_number")
    String certificationNumber;
}
