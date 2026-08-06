package juton113.Medicall.global.external.twilio;

import juton113.Medicall.domain.inquiryLog.InquiryLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/twilio/webhook")
public class TwilioController {

    private final InquiryLogService inquiryLogService;

    // 상대가 전화를 받았을 떄 호출
    @PostMapping(value = "/answer", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleAnswer(@RequestParam(value = "message") String inquiryMessage) {
        return inquiryLogService.generateAnswerTwiML(inquiryMessage);
    }

    // 통화 상대의 말이 끝났다고 Twilio가 판단했을 때 호출
    @PostMapping(value = "/gather", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleGather(@RequestParam("SpeechResult") String hospitalSpeech,
                               @RequestParam("CallSid") String callSid) {
        return inquiryLogService.generateGatherTwiML(callSid, hospitalSpeech);
    }

    @PostMapping(value = "/check-result", produces = MediaType.APPLICATION_XML_VALUE)
    public String handleCheckAiResult(@RequestParam("CallSid") String callSid,
                                      @RequestParam(value = "loop", defaultValue = "1") int loop) {
        return inquiryLogService.generateCheckAiResultTwiML(callSid, loop);
    }

    // 통화 상태가 변할 때 호출
    @PostMapping("/status")
    public void handleCallStatus(@RequestParam("CallSid") String callSid,
                                 @RequestParam("CallStatus") String callStatus,
                                 @RequestParam(value = "SipResponseCode", required = false) String sipCode) {
        inquiryLogService.processCallStatus(callSid, callStatus, sipCode);
    }
}
