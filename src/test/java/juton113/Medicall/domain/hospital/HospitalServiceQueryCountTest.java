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

// N+1 증명용: HospitalService.fetchAndSynchronizeHospitalData()가 병원 건수에 비례해
// findByCode/save를 개별 호출한다는 걸 쿼리 카운트로 검증한다. bulk 처리로 고치기 전까지는 실패해야 정상.
@Transactional
class HospitalServiceQueryCountTest extends QueryCountTestSupport {

    @Autowired
    private HospitalService hospitalService;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("fetchAndSynchronizeHospitalData - 병원 20건 동기화 시 쿼리가 건수에 비례해 발생한다 (N+1)")
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

        // bulk(findAllByCodeIn + saveAll)로 처리하면 건수와 무관하게 상수 수준(여유 있게 5)이어야 함.
        // 지금은 병원 1건당 findByCode + save가 각각 발생해 건수에 비례하므로 이 assert는 실패한다 (N+1 증명).
        assertThat(queryCount).isLessThanOrEqualTo(5);
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
