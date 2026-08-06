package juton113.Medicall.domain.callArtifact;

import jakarta.persistence.*;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import juton113.Medicall.domain.incident.Incident;
import juton113.Medicall.domain.inquiryLog.InquiryLog;
import juton113.Medicall.domain.inquiryLog.enums.CallResponseStatus;
import juton113.Medicall.domain.inquiryLog.enums.RejectionReason;
import juton113.Medicall.global.external.aiGateway.dto.ConversationLog;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Document(collection = "call_artifact")
public class CallArtifact {
    @Id
    private String id; // MongoDB의 "_id" (ObjectID)와 매핑

    @Field("inquiry_id") // 2. RDBMS의 ID를 직접 저장 (JPA @JoinColumn X)
    private Long inquiryId; // InquiryLog의 ID

    @Field("incident_id")
    private Long incidentId;

    @Field("hospital_id")
    private Long hospitalId;

    @Field("twilio_call_sid")
    private String twilioCallSid;

    @Field("call_ended_at")
    private LocalDateTime callEndedAt;

    @Field("response_data") // 3. 중첩 객체 (ResponseData)
    private ResponseData responseData;

    @Field("artifacts") // 4. 중첩 객체 (ArtifactData)
    private ArtifactData artifactData;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    public static class ResponseData {
        @Field("inquery_response_status")
        private CallResponseStatus inqueryResponseStatus;

        @Field("rejection_reason")
        private RejectionReason rejectionReason;
    }

    @Getter
    @Setter
    @Builder
    public static class ArtifactData {
        @Field("recording_url")
        private String recordingUrl;

        @Field("full_transcript")
        private List<ConversationLog> fullTranscript;

        @Field("ai_summary")
        private String aiSummary;
    }

    public static CallArtifact from(InquiryLog inquiryLog,
                                    String callId,
                                    String recordingUrl,
                                    List<ConversationLog> fullTranscript,
                                    String aiSummary) {
        HospitalRanking hospitalRanking = inquiryLog.getHospitalRanking();
        ResponseData responseData = ResponseData.builder()
                .inqueryResponseStatus(inquiryLog.getCallResponseStatus())
                .rejectionReason(inquiryLog.getRejectionReasonCategory())
                .build();
        ArtifactData artifactData = ArtifactData.builder()
                .recordingUrl(recordingUrl)
                .fullTranscript(fullTranscript)
                .aiSummary(aiSummary)
                .build();

        return CallArtifact.builder()
                .inquiryId(inquiryLog.getInquiryLogId())
                .incidentId(hospitalRanking.getSearchSession().getIncident().getIncidentId())
                .hospitalId(hospitalRanking.getHospital().getHospitalId())
                .twilioCallSid(callId)
                .callEndedAt(LocalDateTime.now())
                .responseData(responseData)
                .artifactData(artifactData)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
