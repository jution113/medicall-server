package juton113.Medicall.global.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import juton113.Medicall.domain.incident.dto.Location;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Profile("!perf")
@RequiredArgsConstructor
@Service
public class NemcApiServiceImpl implements NemcApiService {

    private final RestTemplate restTemplate;

    @Override
    public List<NemcHospitalData> getRealtimeHospitalData(
            String asort,
            String rltmEmerCd,
            String rltmCd,
            String svdssCd,
            String rltmMeCd,
            String subTypes,
            Location location,
            int radiusKm) {

        // 1. URL 및 쿼리 파라미터 빌드
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://mediboard.nemc.or.kr")
                .path("/api/v1/search/detail/general")
                .queryParam("searchCondition", "radius")
                .queryParam("lat", location.getLatitude())
                .queryParam("lon", location.getLongitude())
                .queryParam("radius", radiusKm);

        if (asort != null && !asort.isEmpty()) {
            builder.queryParam("asort", asort);
        }
        if (rltmEmerCd != null && !rltmEmerCd.isEmpty()) {
            builder.queryParam("rltmEmerCd", rltmEmerCd);
        }
        if (rltmCd != null && !rltmCd.isEmpty()) {
            builder.queryParam("rltmCd", rltmCd);
        }
        if (svdssCd != null && !svdssCd.isEmpty()) {
            builder.queryParam("svdssCd", svdssCd);
        }
        if (rltmMeCd != null && !rltmMeCd.isEmpty()) {
            builder.queryParam("rltmMeCd", rltmMeCd);
        }
        if (subTypes != null && !subTypes.isEmpty()) {
            builder.queryParam("subTypes", subTypes);
        }

        URI uri = builder.encode().build().toUri();

        // 2. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            // 3. API 호출 및 응답을 DTO로 자동 파싱
            ResponseEntity<NemcApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    NemcApiResponse.class
            );

            // 4. 필요한 값 추출 (result.data)
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                NemcResult result = response.getBody().getResult();
                if (result != null && result.getData() != null) {
                    return result.getData();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("NEMC API 호출 중 오류 발생: " + e.getMessage());
        }

        throw new RuntimeException("유효한 병원 정보를 가져오지 못했습니다.");
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NemcApiResponse {
        private String message;
        private NemcResult result;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NemcResult {
        private int count;
        private List<NemcHospitalData> data;
    }
}
