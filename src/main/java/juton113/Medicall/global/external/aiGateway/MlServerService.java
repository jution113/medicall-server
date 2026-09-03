package juton113.Medicall.global.external.aiGateway;

import juton113.Medicall.global.external.aiGateway.dto.*;

import java.util.List;

public interface MlServerService {
    AiBulkResponse predictRejectionFromMessages(AiPatientInfo aiPatientInfo, List<AiHospitalInfo> aiRequestList);

    InquiryMessageCreateResponse createInquiryMessage(long sessionId, InquiryMessageCreateRequest requestBody);

    ConversationMessageResponse createConversationMessage(long sessionId, String callId);

    AiSummaryCreateResponse createAiSummary(long sessionId, String callId);
}
