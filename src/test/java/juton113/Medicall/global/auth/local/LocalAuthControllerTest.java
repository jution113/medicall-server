package juton113.Medicall.global.auth.local;

import juton113.Medicall.domain.member.Member;
import juton113.Medicall.domain.member.MemberService;
import juton113.Medicall.global.auth.AuthService;
import juton113.Medicall.global.auth.dto.AccessTokenDto;
import juton113.Medicall.global.auth.dto.RefreshTokenDto;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LocalAuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class LocalAuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/local/login - role 생략 시 BASIC 유저로 토큰 발급")
    @WithMockUser
    void login_DefaultsToBasic() throws Exception {
        String basicEmail = LocalTestMember.BY_ROLE.get(Role.BASIC).email();
        given(memberService.findMemberByEmail(basicEmail))
                .willReturn(Member.builder().memberId(1L).build());
        given(authService.issueAccessToken("1")).willReturn(new AccessTokenDto("access-token"));
        given(authService.issueRefreshToken("1")).willReturn(new RefreshTokenDto("refresh-token"));

        mvc.perform(post("/api/v1/auth/local/login").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/local/login?role=ADMIN - ADMIN 유저로 토큰 발급")
    @WithMockUser
    void login_WithAdminRole() throws Exception {
        String adminEmail = LocalTestMember.BY_ROLE.get(Role.ADMIN).email();
        given(memberService.findMemberByEmail(adminEmail))
                .willReturn(Member.builder().memberId(2L).build());
        given(authService.issueAccessToken("2")).willReturn(new AccessTokenDto("admin-access-token"));
        given(authService.issueRefreshToken("2")).willReturn(new RefreshTokenDto("admin-refresh-token"));

        mvc.perform(post("/api/v1/auth/local/login").param("role", "ADMIN").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(2))
                .andExpect(jsonPath("$.accessToken").value("admin-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("admin-refresh-token"));
    }
}
