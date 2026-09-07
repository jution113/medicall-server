package juton113.Medicall.domain.hospitalRanking;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import juton113.Medicall.domain.callArtifact.CallArtifactService;
import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalRepository;
import juton113.Medicall.domain.hospital.enums.HospitalTypeCode;
import juton113.Medicall.domain.hospitalRanking.dto.ScoreDetails;
import juton113.Medicall.domain.incident.Incident;
import juton113.Medicall.domain.incident.IncidentService;
import juton113.Medicall.domain.incident.dto.Location;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import juton113.Medicall.domain.incident.dto.Vitals;
import juton113.Medicall.domain.incident.enums.Consciousness;
import juton113.Medicall.domain.incident.enums.Gender;
import juton113.Medicall.domain.incident.enums.KtasLevel;
import juton113.Medicall.domain.incident.enums.SymptomKeyword;
import juton113.Medicall.domain.inquiryLog.InquiryLog;
import juton113.Medicall.domain.inquiryLog.InquiryLogRepository;
import juton113.Medicall.domain.searchSession.SearchSession;
import juton113.Medicall.domain.searchSession.SearchSessionRepository;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.global.util.RedisService;
import juton113.Medicall.support.QueryCountTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

// N+1 검증용: HospitalRankingService.findRankings()가 조회된 랭킹 건수(N)와 무관하게
// 일정한 쿼리 수(5회)만 실행하는지 검증한다. hospital fetch join, Redis/CallArtifact 벌크 조회,
// 이미 벌크 조회해둔 inquiryLogMap 재사용으로 병원 건수(N)에 비례하던 쿼리를 제거했다.
@Transactional
@TestPropertySource(properties = "logging.level.org.hibernate.SQL=debug")
class HospitalRankingServiceFindRankingsQueryCountTest extends QueryCountTestSupport {

    @Autowired
    private HospitalRankingService hospitalRankingService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalRankingRepository hospitalRankingRepository;

    @Autowired
    private InquiryLogRepository inquiryLogRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private CallArtifactService callArtifactService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("findRankings - 병원 20건 순회 시에도 쿼리가 5회로 고정된다 (N+1 벌크화 검증)")
    void findRankings_QueryCountShouldNotScaleWithHospitalCount() {
        // given
        int hospitalCount = 20;
        List<Hospital> hospitals = saveHospitals(hospitalCount);

        SearchSessionCreateRequest request = sampleRequest(hospitalCount);
        Incident incident = incidentService.createIncident(request);
        SearchSession searchSession = searchSessionRepository.save(SearchSession.from(incident, hospitalCount));

        for (int i = 0; i < hospitals.size(); i++) {
            Hospital hospital = hospitals.get(i);

            ScoreDetails scoreDetails = ScoreDetails.from(300, 5000, List.of(), null, null);
            HospitalRanking hospitalRanking = HospitalRanking.from(searchSession, hospital, 100.0 - i, scoreDetails, null);
            hospitalRanking.setRank(i + 1);
            hospitalRankingRepository.save(hospitalRanking);

            inquiryLogRepository.save(InquiryLog.from(hospitalRanking, "QCT-CALL-SID-" + i));
        }

        given(redisService.getActiveCooldownInfos(anyList(), anyString()))
                .willReturn(Collections.emptyMap());

        given(callArtifactService.getArtifactsByInquiryIds(anyList()))
                .willReturn(Collections.emptyMap());

        // given 단계에서 만든 엔티티가 영속성 컨텍스트에 이미 로드된(non-proxy) 채로 남아있으면
        // hospitalRanking.getHospital() LAZY 로딩이 실제로 트리거되지 않아 N+1이 가려진다.
        // flush + clear로 영속성 컨텍스트를 비워, findRankings()가 진짜 새로 조회하게 만든다.
        entityManager.flush();
        entityManager.clear();

        resetQueryCount();

        // when
        hospitalRankingService.findRankings(searchSession.getSearchSessionId());

        // then
        long queryCount = getQueryCount();
        System.out.printf("[QueryCount] findRankings(병원 %d건) 실행 쿼리 수 = %d%n", hospitalCount, queryCount);

        // 이상적인 쿼리 수 = 5
        //   1) findBySearchSession_searchSessionId (hospital fetch join 시 1회로 유지)
        //   2) getInquiryLogsByRankings (이미 벌크 조회, 유지)
        //   3) getReviews (이미 단건 호출, 유지)
        //   4~5) symptomKeyword 조회(hospitalRankingList.get(0).getSearchSession().getIncident()...) —
        //        루프 밖에서 1회만 호출돼 N과 무관한 고정 비용이라 N+1은 아니지만,
        //        searchSession/incident 둘 다 LAZY라 프록시 초기화 쿼리가 2번 추가로 발생
        assertThat(queryCount).isLessThanOrEqualTo(5);
    }

    private List<Hospital> saveHospitals(int count) {
        List<Hospital> hospitals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Hospital hospital = new Hospital();
            hospital.setCode("QCT-FIND-" + i);
            hospital.setName("쿼리카운트순위조회테스트병원" + i);
            hospital.setHospitalTypeCode(HospitalTypeCode.REGIONAL_EMERGENCY_MEDICAL_CENTER);
            hospital.setAddress("서울시 테스트구 테스트로 " + i);
            hospital.setLocation(Location.of(new BigDecimal("37.5665"), new BigDecimal("126.9780")));
            hospital.setPhoneNumber("02-000-000" + i);
            hospitals.add(hospitalRepository.save(hospital));
        }
        return hospitals;
    }

    private SearchSessionCreateRequest sampleRequest(int searchRadius) {
        PatientInformation patientInformation = PatientInformation.builder()
                .ktas(KtasLevel.LEVEL_1)
                .symptom("쿼리카운트 테스트용 증상 설명")
                .symptomKeyword(SymptomKeyword.CARDIAC_ARREST)
                .age(55)
                .gender(Gender.MALE)
                .consciousness(Consciousness.ALERT)
                .vitals(Vitals.builder()
                        .bpSystolic(120)
                        .bpDiastolic(80)
                        .heartRate(90)
                        .respiratoryRate(18)
                        .bodyTemperature(36.5f)
                        .spo2(97)
                        .bloodSugarTest(100)
                        .build())
                .build();

        return SearchSessionCreateRequest.builder()
                .location(Location.of(new BigDecimal("37.5665"), new BigDecimal("126.9780")))
                .searchRadius(searchRadius)
                .patientInformation(patientInformation)
                .onSiteTreatment("쿼리카운트 테스트용 현장 처치")
                .build();
    }
}
