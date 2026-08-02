package juton113.Atempo.domain.inquiryLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Atempo.domain.reviewLog.ReviewLog;
import juton113.Atempo.domain.reviewLog.ReviewLogService;
import juton113.Atempo.domain.reviewLog.dto.ReviewLogRequest;
import juton113.Atempo.domain.reviewLog.enums.ReviewStatus;
import juton113.Atempo.global.config.MethodSecurityConfig;
import juton113.Atempo.global.config.SecurityConfig;
import juton113.Atempo.global.auth.jwt.JwtAuthenticationFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InquiryLogController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class InquiryLogControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewLogService reviewLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private String reviewRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
                ReviewLogRequest.builder().reviewStatus(ReviewStatus.VERIFIED).comment("ok").build());
    }

    @Test
    @DisplayName("POST .../inquiries/{id}/review-logs - 성공 (VERIFIED)")
    @WithMockUser(roles = "VERIFIED")
    void createInquiryReview_Success() throws Exception {
        given(reviewLogService.createReviewForInquiry(any(), any(), any())).willReturn(new ReviewLog());

        mvc.perform(post("/api/v1/search-sessions/1/inquiries/1/review-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST .../inquiries/{id}/review-logs - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void createInquiryReview_Fail_InsufficientRole() throws Exception {
        mvc.perform(post("/api/v1/search-sessions/1/inquiries/1/review-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH .../inquiries/{id}/review-logs/{id} - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void updateInquiryReview_Success() throws Exception {
        given(reviewLogService.updateReviewLog(any(), any(), any())).willReturn(new ReviewLog());

        mvc.perform(patch("/api/v1/search-sessions/1/inquiries/1/review-logs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isNoContent());
    }
}
