package juton113.Medicall.global.external.twilio;

import juton113.Medicall.domain.inquiryLog.InquiryLog;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

// k6 부하테스트 전용: 실제 Twilio SDK 호출(=실제 전화 발신) 없이 가짜 값만 반환한다.
// initiateCall이 절대 real 구현체로 새어나가면 안 되므로 다른 3개 fake보다 더 중요하게 다룰 것.
@Profile("perf")
@Service
public class FakeTwilioService implements TwilioService {

    @Override
    public String initiateCall(String inquiryMessage, String toPhoneNumber) {
        return "FAKE-CALL-SID-" + UUID.randomUUID();
    }

    @Override
    public String generateAnswerTwiML(String inquiryMessage) {
        return "<Response></Response>";
    }

    @Override
    public String generateGatherTwiML(String callSid, String hospitalSpeech, InquiryLog inquiryLog) {
        return "<Response></Response>";
    }

    @Override
    public String generateCheckAiResultTwiML(String callSid, int loop, InquiryLog inquiryLog) {
        return "<Response></Response>";
    }

    @Override
    public void processCallStatus(String callSid, String callStatus, String sipCode, InquiryLog inquiryLog) {
        // no-op
    }
}
