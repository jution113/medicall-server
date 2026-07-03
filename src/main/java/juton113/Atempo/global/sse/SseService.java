package juton113.Atempo.global.sse;

import juton113.Atempo.domain.reviewLog.dto.ReviewStatusEvent;
import juton113.Atempo.global.sse.dto.InquiryConfirmStatusEvent;
import juton113.Atempo.global.sse.dto.InquiryResultEvent;
import juton113.Atempo.global.sse.dto.InquiryStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {
    private final Long connectionTime;
    private final Map<Long, SseEmitter> emitters;

    public SseService(@Value("${sse.connection-time}") Long connectionTime) {
        this.connectionTime = connectionTime;
        this.emitters = new ConcurrentHashMap<>();
    }

    public SseEmitter subscribe(Long sessionId) {
        SseEmitter emitter = new SseEmitter(connectionTime);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> emitters.remove(sessionId));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // fixedRate = 30000 (30초마다 실행)
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        // 저장된 모든 연결을 돌면서 Ping을 보냄
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("HEARTBEAT")
                        .data("ping"));
            } catch (Exception e) {
                emitters.remove(key);
                 log.warn("Dead connection removed: {}", key);
            }
        });
    }

    public void sendReviewStatus(Long SessionId, ReviewStatusEvent reviewStatusEvent) {
        SseEmitter emitter = emitters.get(SessionId);

        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("REVIEW_STATUS_UPDATED")
                        .data(reviewStatusEvent));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(SessionId);
            }
        }
    }

    public void sendInquiryConfirmStatus(Long SessionId, InquiryConfirmStatusEvent inquiryConfirmStatusEvent) {
        SseEmitter emitter = emitters.get(SessionId);

        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("INQUIRY_CONFIRM_UPDATED")
                        .data(inquiryConfirmStatusEvent));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(SessionId);
            }
        }
    }

    public void sendInquiryStatus(Long SessionId, InquiryStatusEvent inquiryStatusEvent) {
        SseEmitter emitter = emitters.get(SessionId);

        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("INQUIRY_STATUS_CHANGED")
                        .data(inquiryStatusEvent));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(SessionId);
            }
        }
    }

    public void sendInquiryResult(Long SessionId, InquiryResultEvent inquiryResultEvent) {
        SseEmitter emitter = emitters.get(SessionId);

        if(emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("INQUIRY_RESULT_READY")
                        .data(inquiryResultEvent));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(SessionId);
            }
        }
    }
}
