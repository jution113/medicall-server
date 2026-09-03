package juton113.Medicall.global.external.aiGateway;

import juton113.Medicall.global.external.aiGateway.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// k6 부하테스트 전용: 실제 AI 게이트웨이 서버 호출 없이 고정 응답을 즉시 반환한다.
@Profile("perf")
@Service
public class FakeMlServerService implements MlServerService {

    @Override
    public AiBulkResponse predictRejectionFromMessages(AiPatientInfo aiPatientInfo, List<AiHospitalInfo> aiRequestList) {
        List<AiPenaltyInfo> penaltyInfoList = new ArrayList<>();

        for (AiHospitalInfo hospitalInfo : aiRequestList) {
            AiPenaltyInfo penaltyInfo = AiPenaltyInfo.empty();
            penaltyInfo.setHospitalCode(hospitalInfo.getHospitalCode());
            // penaltyReason이 null이면 HospitalRankingService에서 isEmpty() 호출 시 NPE가 나므로 빈 문자열로 채운다.
            penaltyInfo.setPenaltyReason("");
            penaltyInfoList.add(penaltyInfo);
        }

        AiBulkResponse response = new AiBulkResponse();
        response.setHospitalList(penaltyInfoList);
        return response;
    }

    @Override
    public InquiryMessageCreateResponse createInquiryMessage(long sessionId, InquiryMessageCreateRequest requestBody) {
        return InquiryMessageCreateResponse.builder()
                .firstMonologue("안녕하세요, 응급환자 이송 관련하여 문의드립니다.")
                .build();
    }

    @Override
    public ConversationMessageResponse createConversationMessage(long sessionId, String callId) {
        return ConversationMessageResponse.builder()
                .nextAction("HANGUP")
                .conversationMessage("감사합니다.")
                .finalClassification("ACCEPTED")
                .build();
    }

    @Override
    public AiSummaryCreateResponse createAiSummary(long sessionId, String callId) {
        return AiSummaryCreateResponse.builder()
                .aiSummary("k6 perf 프로필 더미 요약")
                .build();
    }
}
