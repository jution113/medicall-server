package juton113.Atempo.global.external.aiGateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Atempo.global.external.aiGateway.dto.ConversationMessageResponse;
import juton113.Atempo.global.util.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiAnalysisService {

    private final MlServerService mlServerService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Async
    public void startAiConversationAnalysis(long sessionId, String callSid) {
        try {
            log.info("비동기 AI 분석 시작: CallSid={}", callSid);

            ConversationMessageResponse response = mlServerService.createConversationMessage(sessionId, callSid);

            // AI 응답 결과를 직렬화하여 Redis에 저장
            String resultJson = objectMapper.writeValueAsString(response);
            redisService.saveAiAnalysisResult(callSid, resultJson);

            log.info("비동기 AI 분석 완료 및 Redis 저장: CallSid={}", callSid);

        } catch (Exception e) {
            log.error("AI 분석 중 오류 발생: CallSid={}", callSid, e);
            // 오류 발생 시에도 Redis에 "오류" 상태를 저장하여 폴링 루프가 인지하도록 함
            try {
                ConversationMessageResponse errorResponse = ConversationMessageResponse.builder()
                        .conversationMessage("AI 응답 중 오류가 발생")
                        .finalClassification("ERROR")
                        .build();

                String errorJson = objectMapper.writeValueAsString(errorResponse);
                redisService.saveAiAnalysisResult(callSid, errorJson);
            } catch (JsonProcessingException jpe) {
                log.error("AI 오류 응답 직렬화 실패", jpe);
            }
        }
    }
}