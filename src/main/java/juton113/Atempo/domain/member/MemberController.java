package juton113.Atempo.domain.member;

import juton113.Atempo.domain.member.dto.*;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("")
    public ResponseEntity<GetMemberResponseDto> getMember(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());

        return ResponseEntity.ok(memberService.getMember(memberId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<GetMemberResponseDto> updateMemberProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                             @RequestBody UpdateMemberProfileRequestDto updateMemberRequestDto) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        UpdateMemberDto updateMemberDto = UpdateMemberDto.builder()
                .memberId(memberId)
                .name(updateMemberRequestDto.getName())
                .nickName(updateMemberRequestDto.getNickName())
                .profileUrl(updateMemberRequestDto.getProfileUrl())
                .build();

        return ResponseEntity.ok(memberService.updateMemberProfile(updateMemberDto));
    }

    @PatchMapping("/certification")
    public ResponseEntity<GetMemberResponseDto> updateMemberCertificationInfo(@AuthenticationPrincipal UserDetails userDetails,
                                                                              @RequestBody UpdateMemberCertificationInfoRequest updateMemberCertificationInfoRequest) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        UpdateMemberCertificationInfoDto updateMemberCertificationInfoDto = UpdateMemberCertificationInfoDto.builder()
                .memberId(memberId)
                .certificationType(updateMemberCertificationInfoRequest.getCertificationType())
                .certificationNumber(updateMemberCertificationInfoRequest.getCertificationNumber())
                .build();

        return ResponseEntity.ok(memberService.updateMemberCertification(updateMemberCertificationInfoDto));
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PatchMapping("/role")
    public ResponseEntity<GetMemberResponseDto> updateMemberRole(@RequestBody UpdateMemberRoleRequestDto updateMemberRequestDto) {
        return ResponseEntity.ok(memberService.updateMemberRole(updateMemberRequestDto));
    }

}
