package juton113.Medicall.domain.inquiryLog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalRepository;
import juton113.Medicall.domain.hospital.enums.HospitalTypeCode;
import juton113.Medicall.domain.hospitalRanking.HospitalRanking;
import juton113.Medicall.domain.hospitalRanking.HospitalRankingRepository;
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
import juton113.Medicall.domain.searchSession.SearchSession;
import juton113.Medicall.domain.searchSession.SearchSessionRepository;
import juton113.Medicall.domain.searchSession.dto.SearchSessionCreateRequest;
import juton113.Medicall.global.external.aiGateway.MlServerService;
import juton113.Medicall.global.external.aiGateway.dto.InquiryMessageCreateResponse;
import juton113.Medicall.global.external.twilio.TwilioService;
import juton113.Medicall.global.sse.SseService;
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

// N+1 검증용: InquiryLogService.createInquiries()의 병원 정보 조회(hospitalRanking.getHospital())가
// fetch join 덕분에 추가 쿼리 없이 처리되는지 검증한다. InquiryLog 저장은 saveAll로 바꿔도
// GenerationType.IDENTITY 때문에 병원 건수만큼 발생하는 게 정상이다.
@Transactional
@TestPropertySource(properties = "logging.level.org.hibernate.SQL=debug")
class InquiryLogServiceCreateInquiriesQueryCountTest extends QueryCountTestSupport {

    @Autowired
    private InquiryLogService inquiryLogService;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalRankingRepository hospitalRankingRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private SearchSessionRepository searchSessionRepository;

    @MockitoBean
    private MlServerService mlServerService;

    @MockitoBean
    private TwilioService twilioService;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private RedisService redisService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("createInquiries - 병원 20건 문의 시에도 병원 조회 쿼리는 추가로 발생하지 않는다 (N+1 벌크화 검증)")
    void createInquiries_QueryCountShouldNotScaleWithHospitalCount() {
        // given
        int hospitalCount = 20;
        List<Hospital> hospitals = saveHospitals(hospitalCount);

        SearchSessionCreateRequest request = sampleRequest(hospitalCount);
        Incident incident = incidentService.createIncident(request);
        SearchSession searchSession = searchSessionRepository.save(SearchSession.from(incident, hospitalCount));

        List<Long> hospitalRankingIds = new ArrayList<>();
        for (int i = 0; i < hospitals.size(); i++) {
            ScoreDetails scoreDetails = ScoreDetails.from(300, 5000, List.of(), null, null);
            HospitalRanking hospitalRanking = HospitalRanking.from(searchSession, hospitals.get(i), 100.0 - i, scoreDetails, null);
            hospitalRanking.setRank(i + 1);
            hospitalRankingIds.add(hospitalRankingRepository.save(hospitalRanking).getHospitalRankingId());
        }

        given(mlServerService.createInquiryMessage(anyLong(), any()))
                .willReturn(InquiryMessageCreateResponse.builder().firstMonologue("문의 시작 안내 멘트").build());

        given(twilioService.initiateCall(anyString(), anyString()))
                .willReturn("QCT-CALL-SID");

        // given 단계에서 만든 HospitalRanking이 영속성 컨텍스트에 이미 로드된(non-proxy) 채로 남아있으면
        // hospitalRanking.getHospital() LAZY 로딩이 실제로 트리거되지 않아 N+1이 가려진다.
        // flush + clear로 영속성 컨텍스트를 비워, findAllById(fetch join 적용)가 진짜 새로 조회하게 만든다.
        entityManager.flush();
        entityManager.clear();

        List<HospitalRanking> targetHospitalRankings = hospitalRankingRepository.findAllById(hospitalRankingIds);

        resetQueryCount();

        // when
        inquiryLogService.createInquiries(searchSession.getSearchSessionId(), incident, targetHospitalRankings);

        // then
        long queryCount = getQueryCount();
        System.out.printf("[QueryCount] createInquiries(병원 %d건) 실행 쿼리 수 = %d%n", hospitalCount, queryCount);

        // 달성 가능한 쿼리 수 = hospitalCount(InquiryLog 저장)
        //   - 병원 정보는 findAllById에 적용된 fetch join 덕분에 이 구간에서 추가 조회가 없다.
        //   - 저장은 InquiryLog가 GenerationType.IDENTITY라 saveAll로 바꿔도 병원 건수만큼 발생한다.
        assertThat(queryCount).isLessThanOrEqualTo(hospitalCount);
    }

    private List<Hospital> saveHospitals(int count) {
        List<Hospital> hospitals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Hospital hospital = new Hospital();
            hospital.setCode("QCT-INQ-" + i);
            hospital.setName("쿼리카운트문의테스트병원" + i);
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
