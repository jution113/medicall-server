package juton113.Medicall.domain.searchSession;

import jakarta.transaction.Transactional;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import juton113.Medicall.domain.hospitalRanking.HospitalRankingService;
import juton113.Medicall.domain.incident.Incident;
import juton113.Medicall.domain.incident.IncidentService;
import juton113.Medicall.domain.incident.dto.HospitalRankingInfo;
import juton113.Medicall.domain.inquiryLog.InquiryLogService;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.domain.searchSession.dto.SearchSessionDetailsResponse;
import juton113.Medicall.global.exception.CustomException;
import juton113.Medicall.global.util.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SearchSessionService {
    private final IncidentService incidentService;
    private final HospitalRankingService hospitalRankingService;
    private final InquiryLogService inquiryLogService;

    private final SearchSessionRepository searchSessionRepository;

    @Transactional
    public SearchSessionDetailsResponse startNewSearch(SearchSessionCreateRequest request) {
        int searchRadius = request.getSearchRadius();

        // Incident 생성
        Incident newIncident = incidentService.createIncident(request);

        // SearchSession 생성
        SearchSession newSearchSession = searchSessionRepository.save(SearchSession.from(newIncident, searchRadius));

        // HospitalRanking List 생성
        List<HospitalRankingInfo> hospitalRankingInfos = hospitalRankingService.createRankings(newSearchSession,
                request.getPatientInformation(),
                request.getLocation());

        return (SearchSessionDetailsResponse.from(newSearchSession.getSearchSessionId(), hospitalRankingInfos));
    }

    @Transactional
    public SearchSessionDetailsResponse findHospitalRankingList(Long sessionId) {
        List<HospitalRankingInfo> hospitalRankingInfos = hospitalRankingService.findRankings(sessionId);

        return (SearchSessionDetailsResponse.from(sessionId, hospitalRankingInfos));
    }

    public SearchSession findSearchSession(Long sessionId) {
        return searchSessionRepository.findById(sessionId).orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
    }

    @Transactional
    public void startInquiry(long sessionId, List<Long> hospitalRankingIds) {
        // 문의 메시지 생성에 필요한 정보 추출
        Incident incident = findSearchSession(sessionId).getIncident();

        List<HospitalRanking> targetHospitalRankings = hospitalRankingService.getHospitalRankingsByIds(hospitalRankingIds);

        inquiryLogService.createInquiries(sessionId, incident, targetHospitalRankings);
    }
}
