package juton113.Medicall.domain.callArtifact;


import juton113.Medicall.domain.inquiryLog.InquiryLog;
import juton113.Medicall.global.external.aiGateway.dto.ConversationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CallArtifactService {
    private final CallArtifactRepository callArtifactRepository;

    public Map<Long, CallArtifact> getArtifactsByInquiryIds(List<Long> inquiryIds) {
        return callArtifactRepository.findByInquiryIdIn(inquiryIds)
                .stream()
                .collect(Collectors.toMap(CallArtifact::getInquiryId, Function.identity()));
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
