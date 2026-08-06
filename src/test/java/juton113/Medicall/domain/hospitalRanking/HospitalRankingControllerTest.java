package juton113.Medicall.domain.hospitalRanking;

import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Medicall.domain.hospitalRanking.dto.InquiryConfirmUpdateRequest;
import juton113.Medicall.domain.reviewLog.ReviewLog;
import juton113.Medicall.domain.reviewLog.ReviewLogService;
import juton113.Medicall.domain.reviewLog.dto.ReviewLogRequest;
import juton113.Medicall.domain.reviewLog.enums.ReviewStatus;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HospitalRankingController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@Import(MethodSecurityConfig.class)
class HospitalRankingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HospitalRankingService hospitalRankingService;

    @MockitoBean
    private ReviewLogService reviewLogService;

    @Autowired
    private ObjectMapper objectMapper;

    private String reviewRequestJson() throws Exception {
        return objectMapper.writeValueAsString(
                ReviewLogRequest.builder().reviewStatus(ReviewStatus.VERIFIED).comment("ok").build());
    }

    @Test
    @DisplayName("POST .../hospital-rankings/{id}/review-logs - 성공 (VERIFIED)")
    @WithMockUser(roles = "VERIFIED")
    void createRankingReview_Success() throws Exception {
        given(reviewLogService.createReviewForRanking(any(), any(), any())).willReturn(new ReviewLog());

        mvc.perform(post("/api/v1/search-sessions/1/hospital-rankings/1/review-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST .../hospital-rankings/{id}/review-logs - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void createRankingReview_Fail_InsufficientRole() throws Exception {
        mvc.perform(post("/api/v1/search-sessions/1/hospital-rankings/1/review-logs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH .../hospital-rankings/{id}/review-logs/{id} - 성공 (ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void updateRankingReview_Success() throws Exception {
        given(reviewLogService.updateReviewLog(any(), any(), any())).willReturn(new ReviewLog());

        mvc.perform(patch("/api/v1/search-sessions/1/hospital-rankings/1/review-logs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequestJson()))
                .andExpect(status().isNoContent());
    }


    @Test
    @DisplayName("PATCH .../hospital-rankings/{id} - 성공 (VERIFIED)")
    @WithMockUser(roles = "VERIFIED")
    void updateInquiryConfirmStatus_Success() throws Exception {
        willDoNothing().given(hospitalRankingService).updateInquiryConfirmStatus(any(), any(), any());

        InquiryConfirmUpdateRequest request = InquiryConfirmUpdateRequest.builder()
                .isConfirmedForInquiry(true)
                .build();

        mvc.perform(patch("/api/v1/search-sessions/1/hospital-rankings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH .../hospital-rankings/{id} - 실패 (BASIC은 권한 없음)")
    @WithMockUser(roles = "BASIC")
    void updateInquiryConfirmStatus_Fail_InsufficientRole() throws Exception {
        InquiryConfirmUpdateRequest request = InquiryConfirmUpdateRequest.builder()
                .isConfirmedForInquiry(true)
                .build();

        mvc.perform(patch("/api/v1/search-sessions/1/hospital-rankings/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}