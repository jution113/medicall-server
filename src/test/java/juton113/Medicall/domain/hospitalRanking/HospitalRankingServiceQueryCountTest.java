package juton113.Medicall.domain.hospitalRanking;

import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalRepository;
import juton113.Medicall.domain.hospital.enums.HospitalTypeCode;
import juton113.Medicall.domain.incident.Incident;
import juton113.Medicall.domain.incident.IncidentService;
import juton113.Medicall.domain.incident.dto.Location;
import juton113.Medicall.domain.incident.dto.PatientInformation;
import juton113.Medicall.domain.incident.dto.Vitals;
import juton113.Medicall.domain.incident.enums.Consciousness;
import juton113.Medicall.domain.incident.enums.Gender;
import juton113.Medicall.domain.incident.enums.KtasLevel;
import juton113.Medicall.domain.incident.enums.SymptomKeyword;
import juton113.Medicall.domain.searchSession.SearchSession;
import juton113.Medicall.domain.searchSession.SearchSessionRepository;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.global.config.SymptomRuleConfig;
import juton113.Medicall.global.external.KakaoMobilityService;
import juton113.Medicall.global.external.NemcApiService;
import juton113.Medicall.global.external.aiGateway.MlServerService;
import juton113.Medicall.global.external.aiGateway.dto.AiBulkResponse;
import juton113.Medicall.global.external.aiGateway.dto.AiPenaltyInfo;
import juton113.Medicall.global.external.dto.EtaResult;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

// N+1 검증용: HospitalRankingService.createRankings()의 병원 조회(findAllByCodeIn)와
// 최근 문의 수 조회(GROUP BY 집계)가 후보 병원 건수(N)와 무관하게 각각 1회만 실행되는지 쿼리 카운트로 검증한다.
// save는 HospitalRanking이 GenerationType.IDENTITY라 saveAll로 바꿔도 병원 건수만큼 발생하는 게 정상이다.
@Transactional
@TestPropertySource(properties = "logging.level.org.hibernate.SQL=debug")
class HospitalRankingServiceQueryCountTest extends QueryCountTestSupport {

    @Autowired
    private HospitalRankingService hospitalRankingService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @Autowired
    private SymptomRuleConfig symptomRuleConfig;

    @MockitoBean
    private NemcApiService nemcApiService;

    @MockitoBean
    private KakaoMobilityService kakaoMobilityService;

    @MockitoBean
    private MlServerService mlServerService;

    @MockitoBean
    private RedisService redisService;

    @Test
    @DisplayName("createRankings - 후보 응급실 20건 처리 시 쿼리가 건수에 비례해 발생한다 (N+1)")
    void createRankings_QueryCountShouldNotScaleWithHospitalCount() {
        // given
        int hospitalCount = 20;
        SymptomKeyword symptomKeyword = SymptomKeyword.CARDIAC_ARREST;
        SymptomRuleConfig.SymptomRule rule = symptomRuleConfig.getRule(symptomKeyword);

        List<Hospital> hospitals = saveHospitals(hospitalCount);
        List<NemcApiService.NemcHospitalData> hospitalDatas = hospitals.stream()
                .map(hospital -> NemcApiService.NemcHospitalData.from(hospital, symptomKeyword, rule))
                .toList();

        SearchSessionCreateRequest request = sampleRequest(hospitalCount, symptomKeyword);
        Incident incident = incidentService.createIncident(request);
        SearchSession searchSession = searchSessionRepository.save(SearchSession.from(incident, hospitalCount));

        given(nemcApiService.getRealtimeHospitalData(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                any(Location.class), anyInt()))
                .willReturn(hospitalDatas);

        given(kakaoMobilityService.getEtaAndDistance(any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class)))
                .willReturn(new EtaResult(300, 5000));

        given(mlServerService.predictRejectionFromMessages(any(), any()))
                .willReturn(emptyPenaltyResponse(hospitals));

        given(redisService.getActiveCooldownInfo(anyString(), anyString()))
                .willReturn(null);

        resetQueryCount();

        // when
        hospitalRankingService.createRankings(searchSession, request.getPatientInformation(), request.getLocation());

        // then
        long queryCount = getQueryCount();
        System.out.printf("[QueryCount] createRankings(병원 %d건) 실행 쿼리 수 = %d%n", hospitalCount, queryCount);

        // 달성 가능한 쿼리 수 = 2(findAllByCodeIn으로 병원 벌크 조회 1 + 최근 문의 수 GROUP BY 집계 1)
        //                    + hospitalCount(저장)
        // 정렬 후 findById는 같은 트랜잭션 내 1차 캐시로 이미 0임을 확인함 (별도 추가 쿼리 없음).
        // 저장이 hospitalCount만큼 나오는 건 saveAll로 바꿔도 못 줄임 — HospitalRanking이
        // GenerationType.IDENTITY라 Hibernate가 INSERT 배치 자체를 구조적으로 비활성화하기 때문.
        assertThat(queryCount).isLessThanOrEqualTo(hospitalCount + 2);
    }

    private List<Hospital> saveHospitals(int count) {
        List<Hospital> hospitals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Hospital hospital = new Hospital();
            hospital.setCode("QCT-RANK-" + i);
            hospital.setName("쿼리카운트순위테스트병원" + i);
            hospital.setHospitalTypeCode(HospitalTypeCode.REGIONAL_EMERGENCY_MEDICAL_CENTER);
            hospital.setAddress("서울시 테스트구 테스트로 " + i);
            hospital.setLocation(Location.of(new BigDecimal("37.5665"), new BigDecimal("126.9780")));
            hospital.setPhoneNumber("02-000-000" + i);
            hospitals.add(hospitalRepository.save(hospital));
        }
        return hospitals;
    }

    private SearchSessionCreateRequest sampleRequest(int searchRadius, SymptomKeyword symptomKeyword) {
        PatientInformation patientInformation = PatientInformation.builder()
                .ktas(KtasLevel.LEVEL_1)
                .symptom("쿼리카운트 테스트용 증상 설명")
                .symptomKeyword(symptomKeyword)
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

    // penaltyReason을 ""로 명시하지 않으면 HospitalRankingService의
    // aiPenalty.getPenaltyReason().isEmpty() 호출에서 NPE가 난다 (AiPenaltyInfo.empty()는 null).
    private AiBulkResponse emptyPenaltyResponse(List<Hospital> hospitals) {
        List<AiPenaltyInfo> penalties = hospitals.stream()
                .map(hospital -> {
                    AiPenaltyInfo penalty = new AiPenaltyInfo();
                    penalty.setHospitalCode(hospital.getCode());
                    penalty.setRejectionProbability(0.0);
                    penalty.setPenaltyReason("");
                    return penalty;
                })
                .toList();

        AiBulkResponse response = new AiBulkResponse();
        response.setHospitalList(penalties);
        return response;
    }
}
