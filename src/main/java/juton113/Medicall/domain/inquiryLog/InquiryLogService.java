package juton113.Medicall.domain.inquiryLog;

import juton113.Medicall.domain.callArtifact.CallArtifact;
import juton113.Medicall.domain.callArtifact.CallArtifactService;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import juton113.Medicall.domain.incident.Incident;
import juton113.Medicall.domain.incident.dto.Artifact;
import juton113.Medicall.domain.incident.dto.InquiryResult;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import juton113.Medicall.domain.inquiryLog.enums.CallStatus;
import juton113.Medicall.domain.reviewLog.ReviewLogRepository;
import juton113.Medicall.domain.searchSession.SearchSessionRepository;
import juton113.Medicall.global.exception.CustomException;
import juton113.Medicall.global.external.aiGateway.MlServerService;
import juton113.Medicall.global.external.aiGateway.dto.InquiryMessageCreateRequest;
import juton113.Medicall.global.external.twilio.TwilioService;
import juton113.Medicall.global.sse.SseService;
import juton113.Medicall.global.sse.dto.InquiryStatusEvent;
import juton113.Medicall.global.util.RedisService;
import juton113.Medicall.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InquiryLogService {
    private final MlServerService mlServerService;
    private final RedisService redisService;
    private final TwilioService twilioService;
    private final CallArtifactService callArtifactService;
    private final SseService sseService;
    private final InquiryLogRepository inquiryLogRepository;
    private final SearchSessionRepository searchSessionRepository;
    private final ReviewLogRepository reviewLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Long> getRecentInquiryCounts(List<String> hospitalCodes, Duration duration) {
        LocalDateTime startTime = LocalDateTime.now().minus(duration);

        return inquiryLogRepository.countGroupedByHospitalCodesAndCallStartedAtAfter(hospitalCodes, startTime)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Transactional
    public InquiryResult getInquiryResult(long hospitalRankingId) {
        InquiryLog inquiryLog = inquiryLogRepository.findByHospitalRanking_hospitalRankingId(hospitalRankingId);

        if (inquiryLog == null) return null;
        long inquiryId = inquiryLog.getInquiryLogId();

        CallArtifact callArtifact = callArtifactService.getArtifactByInquiryId(inquiryId);
        Artifact artifact = callArtifact == null ? null : Artifact.from(callArtifact);

        return InquiryResult.from(inquiryId,
                inquiryLog.getCallStatus(),
                inquiryLog.getCallResponseStatus(),
                inquiryLog.getRejectionReasonCategory(),
                artifact);
    }

    @Transactional
    public InquiryLog getInquiryByCallId(String callId) {
        return inquiryLogRepository.findByCallId(callId).orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_LOG_NOT_FOUND));
    }

    @Transactional
    public void createInquiries(long sessionId, Incident incident, List<HospitalRanking> targetHospitalRankings) {
        // 문의 메시지 생성
        InquiryMessageCreateRequest requestBody = InquiryMessageCreateRequest.builder()
                .patientInformation(PatientInformation.form(incident))
                .onSiteTreatment(incident.getOnSiteTreatment())
                .build();

        redisService.saveInqueryInfo(sessionId, requestBody, Duration.ofHours(1));

        String inquiryMessage = mlServerService.createInquiryMessage(sessionId, requestBody).getFirstMonologue();

        // 문의 시작
        for (HospitalRanking hospitalRanking : targetHospitalRankings) {
            String callId = twilioService.initiateCall(inquiryMessage, hospitalRanking.getHospital().getPhoneNumber());
            inquiryLogRepository.save(InquiryLog.from(hospitalRanking, callId));

            // 문의 직후 상황 전파
            sseService.sendInquiryStatus(sessionId,
                    InquiryStatusEvent.from(hospitalRanking.getHospitalRankingId(),
                            CallStatus.CALLED,
                            null,
                            null)
            );
        }
    }

    public String generateAnswerTwiML(String inquiryMessage) {
        return twilioService.generateAnswerTwiML(inquiryMessage);
    }

    public String generateGatherTwiML(String callSid, String hospitalSpeech) {
        InquiryLog inquiryLog = getInquiryByCallId(callSid);

        return twilioService.generateGatherTwiML(callSid, hospitalSpeech, inquiryLog);
    }

    public String generateCheckAiResultTwiML(String callSid, int loop) {
        InquiryLog inquiryLog = getInquiryByCallId(callSid);

        return twilioService.generateCheckAiResultTwiML(callSid, loop, inquiryLog);
    }

    public void processCallStatus(String callSid, String callStatus, String sipCode) {
        InquiryLog inquiryLog = getInquiryByCallId(callSid);

        twilioService.processCallStatus(callSid, callStatus, sipCode, inquiryLog);
    }

    public List<InquiryLog> getInquiryLogsByRankings(List<HospitalRanking> hospitalRankingList) {
        return inquiryLogRepository.findByHospitalRankingIn(hospitalRankingList);
    }
}
