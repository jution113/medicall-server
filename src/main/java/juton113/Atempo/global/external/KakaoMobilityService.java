package juton113.Atempo.global.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.global.external.dto.EtaResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@Service
public class KakaoMobilityService {

    private final RestTemplate restTemplate;

    @Value("${kakao.mobility.api-key}")
    private String kakaoApiKey;

    public EtaResult getEtaAndDistance(BigDecimal originLon, BigDecimal originLat, BigDecimal destinationLon, BigDecimal destinationLat) {

        // 1. URL 및 쿼리 파라미터 빌드
        String originParam = String.format("%f,%f", originLon, originLat);
        String destinationParam = String.format("%f,%f", destinationLon, destinationLat);

        URI uri = UriComponentsBuilder
                .fromUriString("https://apis-navi.kakaomobility.com")
                .path("/v1/directions")
                .queryParam("origin", originParam)
                .queryParam("destination", destinationParam)
                .queryParam("summary", "true")
                .queryParam("priority", "TIME")
                .encode()
                .build()
                .toUri();

        // 2. HTTP 헤더 설정 (Authorization)
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", kakaoApiKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            // 3. API 호출 및 응답을 DTO로 자동 파싱
            ResponseEntity<KakaoDirectionsResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    KakaoDirectionsResponse.class
            );

            // 4. 필요한 값 추출
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getRoutes() != null && !response.getBody().getRoutes().isEmpty()) {

                Route route = response.getBody().getRoutes().get(0);

                if (route.getResultCode() == 104) {
                    // 5m 이내일 경우 duration과 distance를 1로 설정하여 반환
                    return new EtaResult(1, 1);
                }

                RouteSummary summary = route.getSummary();
                if (summary != null) {
                    return new EtaResult(summary.getDuration(), summary.getDistance());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("카카오 API 호출 중 오류 발생: " + e.getMessage());
        }

        throw new RuntimeException("유효한 ETA 정보를 가져오지 못했습니다.");
    }

    // "카카오 API JSON 응답의 최상위 구조
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KakaoDirectionsResponse {
        private List<Route> routes;
    }

    // "summary" 객체 (필요한 정보만 추출)
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Route {
        @JsonProperty("result_code")
        private int resultCode;

        private RouteSummary summary;
    }

    // "summary" 객체 (필요한 정보만 추출)
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RouteSummary {
        private int distance;
        private int duration;
    }

}
