package juton113.Medicall.global.auth.local;

import juton113.Medicall.domain.member.Member;
import juton113.Medicall.domain.member.MemberService;
import juton113.Medicall.domain.member.dto.CreateMemberDto;
import juton113.Medicall.domain.member.dto.UpdateMemberRoleRequestDto;
import juton113.Medicall.global.auth.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 로컬 개발 전용: 등급(Role)별 인가 분기를 테스트할 수 있도록 role마다 고정 테스트 유저를 기동 시점에 시딩
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalTestMemberInitializer implements CommandLineRunner {
    private final MemberService memberService;

    @Override
    public void run(String... args) {
        LocalTestMember.BY_ROLE.forEach(this::seedMember);
    }

    private void seedMember(Role role, LocalTestMember.LocalTestMemberSpec spec) {
        Member member = memberService.findOrCreateMember(CreateMemberDto.builder()
                .provider(LocalTestMember.PROVIDER)
                .providerId(spec.providerId())
                .name(spec.name())
                .email(spec.email())
                .profileUrl(null)
                .build());

        if (member.getRole() != role) {
            memberService.updateMemberRole(UpdateMemberRoleRequestDto.builder()
                    .memberId(member.getMemberId())
                    .role(role)
                    .build());
        }
    }
}
