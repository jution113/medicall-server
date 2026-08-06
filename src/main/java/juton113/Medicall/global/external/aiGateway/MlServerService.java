package juton113.Medicall.global.external.aiGateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import juton113.Medicall.global.exception.CustomException;
import juton113.Medicall.global.external.aiGateway.dto.*;
import juton113.Medicall.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MlServerService {
    @Value("${api.ai-gateway-server.base-url}")
    private String baseUrl;

    @Value("${api.ai-gateway-server.end-points.analyze-hospital-message}")
    private String analyzeHospitalMessageEndpoint;

    @Value("${api.ai-gateway-server.end-points.create-inquiry-message}")
    private String createInquiryMessageEndpoint;

    @Value("${api.ai-gateway-server.end-points.create-conversation-message}")
    private String createConversationMessageEndpoint;

    @Value("${api.ai-gateway-server.end-points.create-ai-summary}")
    private String createAiSummaryEndpoint;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Transactional
    public AiBulkResponse predictRejectionFromMessages(AiPatientInfo aiPatientInfo, List<AiHospitalInfo> aiRequestList) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiBulkRequest requestBody = AiBulkRequest.builder()
                .patientInfo(aiPatientInfo)
                .hospitalList(aiRequestList)
                .build();

        HttpEntity<AiBulkRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<AiBulkResponse> response = restTemplate.exchange(
                    UriComponentsBuilder.fromUriString(baseUrl)
                            .path(analyzeHospitalMessageEndpoint)
                            .toUriString(),
                    HttpMethod.POST,
                    entity,
                    AiBulkResponse.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomException(ErrorCode.ML_BAD_REQUEST);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ML_SERVER_ERROR);
        }
    }

    @Transactional
    public InquiryMessageCreateResponse createInquiryMessage(long sessionId, InquiryMessageCreateRequest requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<InquiryMessageCreateRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<InquiryMessageCreateResponse> response = restTemplate.exchange(
                    UriComponentsBuilder.fromUriString(baseUrl)
                            .path(createInquiryMessageEndpoint)
                            .buildAndExpand(sessionId)
                            .toUriString(),
                    HttpMethod.POST,
                    entity,
                    InquiryMessageCreateResponse.class
            );

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomException(ErrorCode.ML_BAD_REQUEST);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ML_SERVER_ERROR);
        }
    }

    @Transactional
    public ConversationMessageResponse createConversationMessage(long sessionId, String callId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ConversationMessageResponse> response = restTemplate.exchange(
                    UriComponentsBuilder.fromUriString(baseUrl)
                            .path(createConversationMessageEndpoint)
                            .buildAndExpand(sessionId, callId)
                            .toUriString(),
                    HttpMethod.POST,
                    entity,
                    ConversationMessageResponse.class
            );

            log.info("AI Gateway [Response] (Status: {})", response.getStatusCode());
            log.info("Response Body: {}", objectMapper.writeValueAsString(response.getBody()));

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomException(ErrorCode.ML_BAD_REQUEST);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ML_SERVER_ERROR);
        }
    }

    @Transactional
    public AiSummaryCreateResponse createAiSummary(long sessionId, String callId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AiSummaryCreateResponse> response = restTemplate.exchange(
                    UriComponentsBuilder.fromUriString(baseUrl)
                            .path(createAiSummaryEndpoint)
                            .buildAndExpand(sessionId, callId)
                            .toUriString(),
                    HttpMethod.POST,
                    entity,
                    AiSummaryCreateResponse.class
            );

            log.info("AI Gateway [Response] (Status: {})", response.getStatusCode());
            log.info("Response Body: {}", objectMapper.writeValueAsString(response.getBody()));

            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new CustomException(ErrorCode.ML_BAD_REQUEST);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ML_SERVER_ERROR);
        }
    }
}
