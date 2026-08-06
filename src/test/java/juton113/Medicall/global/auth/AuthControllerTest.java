package juton113.Medicall.global.auth;

import juton113.Medicall.global.auth.jwt.TokenService;
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

import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @DisplayName("GET /api/v1/auth/login - @PreAuthorize 제약 없음 (역할 무관 접근 가능)")
    @WithMockUser(roles = "BASIC")
    void login_NoRoleRestriction() throws Exception {
        mvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/logout - 성공 (BASIC)")
    @WithMockUser(roles = "BASIC")
    void logout_Success() throws Exception {
        willDoNothing().given(authService).logout("test-token");

        mvc.perform(delete("/api/v1/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("DELETE /api/v1/auth/refresh-token/{memberId} - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void invalidateRefreshToken_Success() throws Exception {
        willDoNothing().given(authService).invalidateRefreshToken("1");

        mvc.perform(delete("/api/v1/auth/refresh-token/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/auth/refresh-token/{memberId} - 실패 (VERIFIED는 권한 없음)")
    @WithMockUser(roles = "VERIFIED")
    void invalidateRefreshToken_Fail_InsufficientRole() throws Exception {
        mvc.perform(delete("/api/v1/auth/refresh-token/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
