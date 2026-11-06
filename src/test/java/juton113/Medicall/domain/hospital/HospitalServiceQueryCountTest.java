package juton113.Medicall.domain.hospital;

import juton113.Medicall.support.QueryCountTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

// N+1 검증용: HospitalService.fetchAndSynchronizeHospitalData()의 존재 확인(findAllByCodeIn)이
// 병원 건수(N)와 무관하게 1회만 실행되는지 쿼리 카운트로 검증한다. save는 Hospital이
// GenerationType.IDENTITY라 병원 건수만큼 발생하는 게 정상이다 (saveAll로 모아도 못 줄이는 부분이라
// 개별 save를 그대로 뒀다).
@Transactional
class HospitalServiceQueryCountTest extends QueryCountTestSupport {

    @Autowired
    private HospitalService hospitalService;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("fetchAndSynchronizeHospitalData - 병원 20건 동기화 시에도 존재 확인 쿼리는 1회로 고정된다 (N+1 벌크화 검증)")
    void fetchAndSynchronizeHospitalData_QueryCountShouldNotScaleWithHospitalCount() {
        // given
        int hospitalCount = 20;
        HospitalService.SeoulHospitalApiResponse response = sampleResponse(hospitalCount);

        given(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), isNull(), eq(HospitalService.SeoulHospitalApiResponse.class)))
                .willReturn(ResponseEntity.ok(response));

        resetQueryCount();

        // when
        hospitalService.fetchAndSynchronizeHospitalData();

        // then
        long queryCount = getQueryCount();
        System.out.printf("[QueryCount] fetchAndSynchronizeHospitalData(병원 %d건) 실행 쿼리 수 = %d%n", hospitalCount, queryCount);

        // 달성 가능한 쿼리 수 = 1(findAllByCodeIn으로 존재 확인 벌크 조회) + hospitalCount(저장)
        // 저장은 saveAll로 모아봐도 hospitalCount만큼 그대로 나옴 — Hospital이 GenerationType.IDENTITY라
        // Hibernate가 INSERT 배치 자체를 구조적으로 비활성화하기 때문. 그래서 개별 save를 그대로 뒀다.
        assertThat(queryCount).isLessThanOrEqualTo(hospitalCount + 1);
    }

    private HospitalService.SeoulHospitalApiResponse sampleResponse(int count) {
        List<HospitalService.HospitalRowDto> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            HospitalService.HospitalRowDto dto = new HospitalService.HospitalRowDto();
            dto.setHpid("QCT-TEST-" + i);
            dto.setDutyEmcls("G006");
            dto.setDutyAddr("서울시 테스트구 테스트로 " + i);
            dto.setDutyName("쿼리카운트테스트병원" + i);
            dto.setDutyTel3("02-000-000" + i);
            dto.setWorkDttm("2026-08-09 00:00:00.0");
            rows.add(dto);
        }

        HospitalService.ApiResult result = new HospitalService.ApiResult();
        result.setCode("INFO-000");
        result.setMessage("OK");

        HospitalService.HospitalInfoWrapper wrapper = new HospitalService.HospitalInfoWrapper();
        wrapper.setListTotalCount(count);
        wrapper.setResult(result);
        wrapper.setRows(rows);

        HospitalService.SeoulHospitalApiResponse response = new HospitalService.SeoulHospitalApiResponse();
        response.setHospitalInfo(wrapper);

        return response;
    }
}
