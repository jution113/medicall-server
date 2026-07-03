package juton113.Atempo.global.external.aiGateway;

import juton113.Atempo.domain.callArtifact.CallArtifact;
import juton113.Atempo.domain.callArtifact.CallArtifactService;
import juton113.Atempo.domain.hospitalRanking.HospitalRanking;
import juton113.Atempo.domain.inquiryLog.InquiryLog;
import juton113.Atempo.domain.inquiryLog.InquiryLogRepository;
import juton113.Atempo.domain.inquiryLog.InquiryLogService;
import juton113.Atempo.domain.inquiryLog.enums.CallResponseStatus;
import juton113.Atempo.domain.inquiryLog.enums.CallStatus;
import juton113.Atempo.domain.inquiryLog.enums.RejectionReason;
import juton113.Atempo.domain.searchSession.SearchSession;
import juton113.Atempo.global.exception.CustomException;
import juton113.Atempo.global.sse.SseService;
import juton113.Atempo.global.sse.dto.InquiryResultEvent;
import juton113.Atempo.global.sse.dto.InquiryStatusEvent;
import juton113.Atempo.global.util.RedisService;
import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostCallProcessingService {

    private final CallArtifactService callArtifactService;
    private final RedisService redisService;
    private final SseService sseService;
    private final MlServerService mlServerService;
    private final InquiryLogRepository inquiryLogRepository;

    @Async
    @Transactional
    public void processFinalCallData(Long inquiryLogId, String finalClassification, String callSid) {
        try {
            log.info("비동기 후처리 시작: CallSid={}", callSid);

            // 비동기 작업 시 닫힌 세션에 조회하는 것을 방지하기 위함
            InquiryLog inquiryLog = inquiryLogRepository.findById(inquiryLogId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_LOG_NOT_FOUND));
            HospitalRanking hospitalRanking = inquiryLog.getHospitalRanking();
            SearchSession searchSession = hospitalRanking.getSearchSession();
            long hospitalRankingId = hospitalRanking.getHospitalRankingId();
            long sessionId = searchSession.getSearchSessionId();

            // 통화 상태 및 결과 업데이트
            inquiryLog.setCallStatus(CallStatus.ANSWERED);

            if (finalClassification.equals(CallResponseStatus.ACCEPTED.toString())) {
                // 수용
                inquiryLog.setCallResponseStatus(CallResponseStatus.ACCEPTED);
            } else {
                // 거절
                inquiryLog.setCallResponseStatus(CallResponseStatus.REJECTED);
                inquiryLog.setRejectionReasonCategory(RejectionReason.fromString(finalClassification));
                RejectionReason rejectionReason = RejectionReason.fromString(finalClassification);

                // 쿨다운 적용
                redisService.addRejectionCooldown(hospitalRanking.getHospital().getCode(),
                        searchSession.getIncident().getPatientSymptomKeyword().getKeyword(),
                        rejectionReason,
                        rejectionReason.getCooldownDuration());
            }

            String aiSummary = "요약 생성 실패";
            try {
                aiSummary = mlServerService.createAiSummary(sessionId, callSid).getAiSummary();
            } catch (Exception e) {
                log.error("AI 요약 API 호출 실패: CallSid={}", callSid, e);
            }

            // 통화 결과 생성
            // TODO: S3에 저장할  것
            CallArtifact callArtifact = callArtifactService.createCallArtifact(inquiryLog,
                    callSid,
                    null,
                    redisService.getConversationLogs(callSid),
                    aiSummary
            );

            // SSE 전파
            // 문의 상태 전파
            sseService.sendInquiryStatus(sessionId,
                    InquiryStatusEvent.from(hospitalRankingId,
                            inquiryLog.getCallStatus(),
                            inquiryLog.getCallResponseStatus(),
                            inquiryLog.getRejectionReasonCategory()
                    )
            );

            // 통화 결과 전파
            sseService.sendInquiryResult(sessionId,
                    InquiryResultEvent.from(hospitalRankingId,
                            callArtifact
                    )
            );

            log.info("비동기 후처리 완료: CallSid={}", callSid);

        } catch (Exception e) {
            log.error("비동기 후처리 중 오류 발생: CallSid={}", callSid, e);
        }
    }
}