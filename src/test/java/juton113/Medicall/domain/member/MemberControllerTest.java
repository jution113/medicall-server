package juton113.Medicall.domain.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Medicall.domain.member.dto.GetMemberResponseDto;
import juton113.Medicall.domain.member.dto.UpdateMemberCertificationInfoRequest;
import juton113.Medicall.domain.member.dto.UpdateMemberProfileRequestDto;
import juton113.Medicall.domain.member.dto.UpdateMemberRoleRequestDto;
import juton113.Medicall.global.auth.enums.Role;
import juton113.Medicall.global.config.MethodSecurityConfig;
import juton113.Medicall.global.config.SecurityConfig;
import juton113.Medicall.global.auth.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private GetMemberResponseDto sampleResponse() {
        return GetMemberResponseDto.builder()
                .name("testUser")
                .nickName("testNick")
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/members - 성공 (BASIC)")
    @WithMockUser(username = "1", roles = "BASIC")
    void getMember_Success() throws Exception {
        given(memberService.getMember(any())).willReturn(sampleResponse());

        mvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/profile - 성공 (VERIFIED)")
    @WithMockUser(username = "1", roles = "VERIFIED")
    void updateMemberProfile_Success() throws Exception {
        given(memberService.updateMemberProfile(any())).willReturn(sampleResponse());

        UpdateMemberProfileRequestDto request = UpdateMemberProfileRequestDto.builder()
                .name("newName")
                .nickName("newNick")
                .build();

        mvc.perform(patch("/api/v1/members/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/certification - 성공 (ADMIN)")
    @WithMockUser(username = "1", roles = "ADMIN")
    void updateMemberCertificationInfo_Success() throws Exception {
        given(memberService.updateMemberCertification(any())).willReturn(sampleResponse());

        UpdateMemberCertificationInfoRequest request = UpdateMemberCertificationInfoRequest.builder()
                .certificationNumber("12345")
                .build();

        mvc.perform(patch("/api/v1/members/certification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/certification - 실패 (VERIFIED는 권한 없음)")
    @WithMockUser(roles = "VERIFIED")
    void updateMemberCertificationInfo_Fail_InsufficientRole() throws Exception {
        UpdateMemberCertificationInfoRequest request = UpdateMemberCertificationInfoRequest.builder()
                .certificationNumber("12345")
                .build();

        mvc.perform(patch("/api/v1/members/certification")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/role - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void updateMemberRole_Success() throws Exception {
        given(memberService.updateMemberRole(any())).willReturn(sampleResponse());

        UpdateMemberRoleRequestDto request = UpdateMemberRoleRequestDto.builder()
                .memberId(1L)
                .role(Role.VERIFIED)
                .build();

        mvc.perform(patch("/api/v1/members/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/role - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void updateMemberRole_Fail_InsufficientRole() throws Exception {
        UpdateMemberRoleRequestDto request = UpdateMemberRoleRequestDto.builder()
                .memberId(1L)
                .role(Role.ADMIN)
                .build();

        mvc.perform(patch("/api/v1/members/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
