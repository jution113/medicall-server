package juton113.Medicall.global.sse;

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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SseController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class SseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SseService sseService;

    @Test
    @DisplayName("GET /api/v1/notifications/subscribe - 성공 (VERIFIED)")
    @WithMockUser(username = "1", roles = "VERIFIED")
    void subscribe_Success() throws Exception {
        given(sseService.subscribe(anyLong())).willReturn(new SseEmitter());

        mvc.perform(get("/api/v1/notifications/subscribe").param("sessionId", "1"))
                .andExpect(request().asyncStarted());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/subscribe - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void subscribe_Fail_InsufficientRole() throws Exception {
        mvc.perform(get("/api/v1/notifications/subscribe").param("sessionId", "1"))
                .andExpect(status().isForbidden());
    }
}
