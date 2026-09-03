package juton113.Medicall.global.external.twilio;

import juton113.Medicall.domain.inquiryLog.InquiryLog;

public interface TwilioService {
    String initiateCall(String inquiryMessage, String toPhoneNumber);

    String generateAnswerTwiML(String inquiryMessage);

    String generateGatherTwiML(String callSid, String hospitalSpeech, InquiryLog inquiryLog);

    String generateCheckAiResultTwiML(String callSid, int loop, InquiryLog inquiryLog);

    void processCallStatus(String callSid, String callStatus, String sipCode, InquiryLog inquiryLog);
}
