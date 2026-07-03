package juton113.Atempo.global.auth;

import juton113.Atempo.global.auth.dto.AccessTokenDto;
import juton113.Atempo.global.auth.dto.LoginResponseDto;
import juton113.Atempo.global.auth.dto.RefreshTokenDto;
import juton113.Atempo.global.util.enums.ErrorCode;
import juton113.Atempo.global.exception.CustomException;
import juton113.Atempo.global.auth.jwt.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    @GetMapping("/login")
    public ResponseEntity<LoginResponseDto> login() {
        String loginUrl = "/oauth2/authorization/google";

        return ResponseEntity.ok(new LoginResponseDto("Returns OAuth login endpoint.", loginUrl));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorization) {
        String accessToken = resolveToken(authorization);
        authService.logout(accessToken);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/access-token")
    public ResponseEntity<?> reissueAccessToken(@RequestHeader("Authorization") String authorization) {
        String refreshToken = resolveToken(authorization);
        AccessTokenDto accessTokenDto = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + accessTokenDto.getAccessToken())
                .body("AccessToken Reissued");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> reissueRefreshToken(@RequestHeader("Authorization") String authorization) {
        String refreshToken = resolveToken(authorization);
        RefreshTokenDto refreshTokenDto = authService.reissueRefreshToken(refreshToken);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + refreshTokenDto.getRefreshToken())
                .body("RefreshToken Reissued");
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/refresh-token/{memberId}")
    private ResponseEntity<?> invalidateRefreshToken(@PathVariable("memberId") String memberId) {
        authService.invalidateRefreshToken(memberId);

        return ResponseEntity.noContent().build();
    }

    private String resolveToken(String bearerToken) {
        if (bearerToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        } else if (!bearerToken.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.INVALID_AUTH_HEADER_FORMAT);
        }

        return bearerToken.substring(7);
    }

}
