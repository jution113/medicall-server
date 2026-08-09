package juton113.Medicall.global.auth.local;

import juton113.Medicall.domain.member.Member;
import juton113.Medicall.domain.member.MemberService;
import juton113.Medicall.global.auth.AuthService;
import juton113.Medicall.global.auth.dto.AccessTokenDto;
import juton113.Medicall.global.auth.dto.RefreshTokenDto;
import juton113.Medicall.global.auth.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 로컬 개발 전용: 구글 OAuth2 로그인 없이 role별 시딩된 테스트 유저로 실제 JWT를 즉시 발급받기 위한 엔드포인트
@Profile("local")
@RestController
@RequestMapping("/api/v1/auth/local")
@RequiredArgsConstructor
public class LocalAuthController {
    private final MemberService memberService;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LocalLoginResponseDto> login(@RequestParam(defaultValue = "BASIC") Role role) {
        LocalTestMember.LocalTestMemberSpec spec = LocalTestMember.BY_ROLE.get(role);
        Member member = memberService.findMemberByEmail(spec.email());
        String memberId = String.valueOf(member.getMemberId());

        AccessTokenDto accessTokenDto = authService.issueAccessToken(memberId);
        RefreshTokenDto refreshTokenDto = authService.issueRefreshToken(memberId);

        return ResponseEntity.ok(new LocalLoginResponseDto(
                member.getMemberId(),
                accessTokenDto.getAccessToken(),
                refreshTokenDto.getRefreshToken()
        ));
    }
}
