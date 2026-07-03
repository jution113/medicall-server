package juton113.Atempo.domain.searchSession;

import juton113.Atempo.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Atempo.domain.searchSession.dto.SearchSessionDetailsResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/search-sessions")
public class SearchSessionController {
    private final SearchSessionService searchSessionService;

//    @PreAuthorize("hasAnyRole('ADMIN', VERIFIED)")
    @PostMapping("")
    public ResponseEntity<SearchSessionDetailsResponse> startSearchSession(/* @AuthenticationPrincipal UserDetails userDetails, */
                                                                   @RequestBody SearchSessionCreateRequest request) {
//        Long memberId = Long.parseLong(userDetails.getUsername());

        SearchSessionDetailsResponse response = searchSessionService.startNewSearch(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}/hospital-rankings")
    public ResponseEntity<SearchSessionDetailsResponse> findHospitalRankingList(@PathVariable("sessionId") Long sessionId) {
        SearchSessionDetailsResponse response = searchSessionService.findHospitalRankingList(sessionId);

        return ResponseEntity.ok(response);
    }

// ----- ----- ----- ----- ----- [PROD] ----- ----- ----- ----- -----
    @PostMapping("/{sessionId}/inquiries")
    public ResponseEntity<Void> startInquiry(@PathVariable("sessionId") long sessionId,
                                             @RequestBody List<Long> hospitalRankingIds) {
//        searchSessionService.startInquiry(sessionId, hospitalRankingIds);
        return ResponseEntity.accepted().build();
    }

// ----- ----- ----- ----- ----- [TEST] ----- ----- ----- ----- -----
    @PostMapping("/{sessionId}/inquiries/test")
    public ResponseEntity<Void> startTestInquiry(@PathVariable("sessionId") long sessionId,
                                             @RequestBody List<Long> hospitalRankingIds) {
        searchSessionService.startTestInquiry(sessionId, hospitalRankingIds);
        return ResponseEntity.accepted().build();
    }
}
