package juton113.Medicall.domain.callArtifact;


import juton113.Medicall.domain.inquiryLog.InquiryLog;
import juton113.Medicall.global.external.aiGateway.dto.ConversationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CallArtifactService {
    private final CallArtifactRepository callArtifactRepository;

    public CallArtifact getArtifactByInquiryId(Long inquiryLogId) {

        Optional<CallArtifact> optionalArtifact = callArtifactRepository.findByInquiryId(inquiryLogId);

        return optionalArtifact.orElse(null);
    }

    public CallArtifact createCallArtifact(InquiryLog inquiryLog,
                                           String callId,
                                           String recordingUrl,
                                           List<ConversationLog> fullTranscript,
                                           String aiSummary) {
        return callArtifactRepository.save(
                CallArtifact.from(inquiryLog,
                        callId,
                        recordingUrl,
                        fullTranscript,
                        aiSummary
                )
        );
    }
}
