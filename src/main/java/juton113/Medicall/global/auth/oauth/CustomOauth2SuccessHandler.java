package juton113.Medicall.global.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import juton113.Medicall.global.auth.dto.AccessTokenDto;
import juton113.Medicall.global.auth.dto.RefreshTokenDto;
import juton113.Medicall.global.auth.AuthService;
import juton113.Medicall.domain.member.MemberService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@AllArgsConstructor
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {
    private final MemberService memberService;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttributes().get("email");
        String memberId = String.valueOf(memberService.findMemberByEmail(email).getMemberId());

        AccessTokenDto accessTokenDto = authService.issueAccessToken(memberId);
        RefreshTokenDto refreshTokenDto = authService.issueRefreshToken(memberId);

        // TODO: 웹/앱 로그인 구분용 임시 디버깅 코드 (User-Agent 기반 추정, 정리 대상)
        if (isWebRequest(request)) {
            response.setHeader("atk", accessTokenDto.getAccessToken());
            response.setHeader("rtk", refreshTokenDto.getRefreshToken());
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String redirectUri = UriComponentsBuilder.fromUriString("medicall://auth")
                .queryParam("atk", accessTokenDto.getAccessToken())
                .queryParam("rtk", refreshTokenDto.getRefreshToken())
                .build()
                .toUriString();

        response.sendRedirect(redirectUri);
    }

    private boolean isWebRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.contains("wv");
    }
}
