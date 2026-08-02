package juton113.Atempo.global.sse;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@AllArgsConstructor
@RequestMapping("/api/v1/notifications")
@RestController
public class SseController {
    private final SseService sseService;

    @PreAuthorize("hasAnyRole('ADMIN', 'VERIFIED')")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam long sessionId) {
         Long memberId = Long.parseLong(userDetails.getUsername());

        return sseService.subscribe(sessionId);
    }

}
