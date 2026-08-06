package juton113.Medicall.domain.hospital;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HospitalController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class HospitalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HospitalService hospitalService;

    @Test
    @DisplayName("POST /api/v1/hospitals/refresh - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void refreshHospitalData_Success() throws Exception {
        willDoNothing().given(hospitalService).fetchAndSynchronizeHospitalData();

        mvc.perform(post("/api/v1/hospitals/refresh").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/hospitals/refresh - 실패 (VERIFIED는 권한 없음)")
    @WithMockUser(roles = "VERIFIED")
    void refreshHospitalData_Fail_InsufficientRole() throws Exception {
        mvc.perform(post("/api/v1/hospitals/refresh").with(csrf()))
                .andExpect(status().isForbidden());
    }
}