package juton113.Medicall.domain.searchSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.domain.searchSession.dto.SearchSessionDetailsResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SearchSessionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class SearchSessionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchSessionService searchSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    private SearchSessionDetailsResponse sampleResponse() {
        return SearchSessionDetailsResponse.builder()
                .searchSessionId(1L)
                .totalCount(0)
                .reviewedCount(0)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/search-sessions - 성공 (VERIFIED)")
    @WithMockUser(username = "1", roles = "VERIFIED")
    void startSearchSession_Success() throws Exception {
        given(searchSessionService.startNewSearch(any())).willReturn(sampleResponse());

        SearchSessionCreateRequest request = SearchSessionCreateRequest.builder()
                .searchRadius(3000)
                .build();

        mvc.perform(post("/api/v1/search-sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/search-sessions - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void startSearchSession_Fail_InsufficientRole() throws Exception {
        SearchSessionCreateRequest request = SearchSessionCreateRequest.builder()
                .searchRadius(3000)
                .build();

        mvc.perform(post("/api/v1/search-sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/search-sessions/{id}/hospital-rankings - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void findHospitalRankingList_Success() throws Exception {
        given(searchSessionService.findHospitalRankingList(any())).willReturn(sampleResponse());

        mvc.perform(get("/api/v1/search-sessions/1/hospital-rankings"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/search-sessions/{id}/hospital-rankings - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void findHospitalRankingList_Fail_InsufficientRole() throws Exception {
        mvc.perform(get("/api/v1/search-sessions/1/hospital-rankings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/search-sessions/{id}/inquiries - 성공 (VERIFIED)")
    @WithMockUser(roles = "VERIFIED")
    void startInquiry_Success() throws Exception {
        mvc.perform(post("/api/v1/search-sessions/1/inquiries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /api/v1/search-sessions/{id}/inquiries - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void startInquiry_Fail_InsufficientRole() throws Exception {
        mvc.perform(post("/api/v1/search-sessions/1/inquiries")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isForbidden());
    }

}
