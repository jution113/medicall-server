package juton113.Medicall.global.auth.local;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocalLoginResponseDto {
    private Long memberId;
    private String accessToken;
    private String refreshToken;
}
