package juton113.Atempo.global.external.twilio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.http.HttpMethod;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.type.PhoneNumber;
import jakarta.transaction.Transactional;
import juton113.Atempo.domain.callArtifact.CallArtifactService;
import juton113.Atempo.domain.hospitalRanking.HospitalRanking;
import juton113.Atempo.domain.inquiryLog.InquiryLog;
import juton113.Atempo.domain.inquiryLog.enums.CallStatus;
import juton113.Atempo.domain.searchSession.SearchSession;
import juton113.Atempo.global.external.aiGateway.AiAnalysisService;
import juton113.Atempo.global.external.aiGateway.MlServerService;
import juton113.Atempo.global.external.aiGateway.PostCallProcessingService;
import juton113.Atempo.global.external.aiGateway.dto.ConversationLog;
import juton113.Atempo.global.external.aiGateway.dto.ConversationMessageResponse;
import juton113.Atempo.global.sse.SseService;
import juton113.Atempo.global.sse.dto.InquiryStatusEvent;
import juton113.Atempo.global.util.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class TwilioService {
    private final MlServerService mlServerService;
    private final ObjectMapper objectMapper;
    private final AiAnalysisService aiAnalysisService;
    private final PostCallProcessingService postCallProcessingService;
    private final CallArtifactService callArtifactService;
    private final RedisService redisService;
    private final SseService sseService;

    @Value("${api.api-server.base-url}")
    private String baseUrl;

    @Value("${twilio.from-phone-number}")
    private String fromPhoneNumber;

    public String initiateCall(String inquiryMessage, String toPhoneNumber) {
        try {
            String encodedMessage = URLEncoder.encode(inquiryMessage, StandardCharsets.UTF_8);
            URI voiceUrl = URI.create(baseUrl + "/api/v1/twilio/webhook/answer?message=" + encodedMessage);
            URI statusCallbackUrl = URI.create(baseUrl + "/api/v1/twilio/webhook/status");

            Call call = Call.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    voiceUrl)
                    .setStatusCallback(statusCallbackUrl)
                    .setStatusCallbackMethod(HttpMethod.POST)
                    .setStatusCallbackEvent("completed")
                    .create();

            log.info("Twilio 전화 걸기 성공: CallSid={}, To={}", call.getSid(), toPhoneNumber);
            return call.getSid();

        } catch (Exception e) {
            log.error("Twilio 전화 걸기 실패: To={}, Message={}", toPhoneNumber, inquiryMessage, e);
            throw new RuntimeException("Twilio 전화 걸기 중 오류 발생");
        }
    }

    public String generateAnswerTwiML(String inquiryMessage) {
        VoiceResponse response = new VoiceResponse.Builder()
                .gather(new Gather.Builder()
                        .inputs(Gather.Input.SPEECH)
                        .language(Gather.Language.KO_KR)
                        .action(baseUrl + "/api/v1/twilio/webhook/gather")
                        .method(HttpMethod.POST)
                        .speechTimeout("auto")
                        .say(new Say.Builder(inquiryMessage)
                                .language(Say.Language.KO_KR)
                                .voice(Say.Voice.POLLY_SEOYEON)
                                .build())
                        .build())
                .say(new Say.Builder("응답을 듣지 못했습니다. 다시 걸어주세요.")
                        .language(Say.Language.KO_KR).build())
                .hangup(new Hangup.Builder().build())
                .build();

        return response.toXml();
    }

    @Transactional
    public String generateGatherTwiML(String callSid, String hospitalSpeech, InquiryLog inquiryLog) {
        HospitalRanking hospitalRanking = inquiryLog.getHospitalRanking();
        SearchSession searchSession = hospitalRanking.getSearchSession();

        long sessionId = searchSession.getSearchSessionId();

        // 병원의 대답 저장
        redisService.saveConversation(callSid, ConversationLog.from("hospital", hospitalSpeech), Duration.ofHours(1));

        // 병원의 대답 조회 키 전달 및 다음 메시지 요청
        aiAnalysisService.startAiConversationAnalysis(sessionId, callSid);

        VoiceResponse.Builder responseBuilder = new VoiceResponse.Builder();
        Say say = new Say.Builder("답변을 생성 중입니다.")
                .language(Say.Language.KO_KR)
                .voice(Say.Voice.POLLY_SEOYEON)
                .build();

        responseBuilder.say(say);
        responseBuilder.pause(new Pause.Builder().length(3).build()); // 3초 대기

        // 3초 후, AI 결과를 확인하는 새 엔드포인트(/check-result)로 리다이렉트
        responseBuilder.redirect(new Redirect.Builder(baseUrl + "/api/v1/twilio/webhook/check-result?loop=1")
                .method(HttpMethod.POST)
                .build());

        return responseBuilder.build().toXml();
    }

    @Transactional
    public String generateCheckAiResultTwiML(String callSid, int loop, InquiryLog inquiryLog) {
        final int MAX_LOOPS = 5;

        // Redis에서 AI 응답 결과 조회
        String aiAnalysisResult = redisService.getAndDeleteAiAnalysisResult(callSid);
        VoiceResponse.Builder responseBuilder = new VoiceResponse.Builder();

        if (aiAnalysisResult != null) {
            // 분석 완료, 성공
            log.info("AI 폴링 성공: CallSid={}", callSid);

            try {
                // 통화 종료 여부 판단 및 db 업데이트
                ConversationMessageResponse response = objectMapper.readValue(aiAnalysisResult, ConversationMessageResponse.class);
                String finalClassification = response.getFinalClassification();
                String assistantSpeech = response.getConversationMessage();
                Say say = new Say.Builder(assistantSpeech).language(Say.Language.KO_KR).voice(Say.Voice.POLLY_SEOYEON).build();

                // AI 응답 저장
                redisService.saveConversation(callSid, ConversationLog.from("assistant", assistantSpeech), Duration.ofHours(1));

                if (finalClassification != null) {
                    // 통화 종료 후 작업 비동기 처리 시작
                    postCallProcessingService.processFinalCallData(inquiryLog.getInquiryLogId(), finalClassification, callSid);
                    responseBuilder.say(say).hangup(new Hangup.Builder().build());
                } else {
                    responseBuilder.gather(new Gather.Builder()
                            .inputs(Gather.Input.SPEECH)
                            .language(Gather.Language.KO_KR)
                            .action(baseUrl + "/api/v1/twilio/webhook/gather")
                            .method(HttpMethod.POST)
                            .speechTimeout("auto")
                            .say(say)
                            .build());
                }
            } catch (JsonProcessingException e) {
                log.error("AI 결과 역직렬화 실패", e);
                throw new RuntimeException(e);
            }

        } else {
            // [결과 아직 없음]
            log.warn("AI 폴링 대기 중: CallSid={}, Loop={}", callSid, loop);

            if (loop >= MAX_LOOPS) {
                // 타임아웃 처리
                Say say = new Say.Builder("AI 분석이 너무 오래 걸려 통화를 종료합니다.")
                        .language(Say.Language.KO_KR).build();
                responseBuilder.say(say).hangup(new Hangup.Builder().build());
            } else {
                // 3초 더 기다린 후, 이 엔드포인트를 다시 호출 (루프 카운터 증가)
                Say say = new Say.Builder("분석 중입니다.").language(Say.Language.KO_KR).build();
                responseBuilder.say(say);
                responseBuilder.pause(new Pause.Builder().length(3).build());

                responseBuilder.redirect(new Redirect.Builder(baseUrl + "/api/v1/twilio/webhook/check-result?loop=" + (loop + 1))
                        .method(HttpMethod.POST)
                        .build());
            }
        }

        return responseBuilder.build().toXml();
    }

    @Transactional
    public void processCallStatus(String callSid, String callStatus, String sipCode, InquiryLog inquiryLog) {
        if (callStatus.equals("busy") || callStatus.equals("no-answer"))
        {
            HospitalRanking hospitalRanking = inquiryLog.getHospitalRanking();
            inquiryLog.setCallStatus(CallStatus.NO_ANSWER);

            sseService.sendInquiryStatus(
                    hospitalRanking.getSearchSession().getSearchSessionId(),
                    InquiryStatusEvent.from(hospitalRanking.getHospitalRankingId(),
                            inquiryLog.getCallStatus(),
                            inquiryLog.getCallResponseStatus(),
                            inquiryLog.getRejectionReasonCategory()
                    )
            );
        }
    }

}
