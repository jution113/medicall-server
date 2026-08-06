package juton113.Medicall.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.global.auth.enums.Role;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRoleRequestDto {
    @JsonProperty("member_id")
    Long memberId;

    Role role;
}
