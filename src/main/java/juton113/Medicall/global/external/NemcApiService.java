package juton113.Medicall.global.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.incident.dto.Location;
import juton113.Medicall.domain.incident.enums.SymptomKeyword;
import juton113.Medicall.global.config.SymptomRuleConfig;
import lombok.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class NemcApiService {

    private final RestTemplate restTemplate;

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
    public static class NemcApiResponse {
        private String message;
        private NemcResult result;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcResult {
        private int count;
        private List<NemcHospitalData> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcHospitalData {
        @JsonProperty("code")
        private String hospitalCode; // (정부 기관 ID)
        private String name;
        private String address;
        private double distance;

         // API 응답 본문의 typeCode 필드
        private String typeCode;

         // API 응답 본문의 subTypes 필드
        private List<String> subTypes;

        // 실시간 병상 정보
        private NemcElementMap rltmEmerCd;

        // 실시간 진료과 정보
        private NemcElementMap rltmCd;

        // 중증 응급 질환
        private NemcElementMap svdssCd;

        // 실시간 의료 장비
        private NemcElementMap rltmMeCd;

        // 병원 공지 메시지 (수용 불가 사유 등)
        private List<NemcMessage> erMessages;
        private List<NemcUnavailableMessage> unavailableMessages;

        // 더미 데이터 생성 헬퍼 메서드
        private static NemcElementMap createElementMapFromRules(List<SymptomRuleConfig.ScoringRuleItem> rules, NemcAvailabilityInfo defaultInfo) {

            // 1. 규칙에 있는 모든 코드를 맵에 추가
            Map<String, NemcAvailabilityInfo> elements = new HashMap<>();
            if (rules != null) {
                for (SymptomRuleConfig.ScoringRuleItem item : rules) {
                    elements.put(item.getCode(), defaultInfo);
                }
            }

            // 2. NemcElementMap DTO 생성
            NemcElementMap elementMap = new NemcElementMap();
            elementMap.setElements(elements);
            elementMap.setHasElement(!elements.isEmpty());
            elementMap.setBlocked(false);

            return elementMap;
        }

        // 더미 데이터 생성 메서드
        public static NemcHospitalData from(Hospital hospital,
                                            SymptomKeyword symptomKeyword,
                                            SymptomRuleConfig.SymptomRule rule) {

            SymptomRuleConfig.ScoringRuleMap scoringRules = rule.getScoringRules();

            // 1. 점수 계산을 통과할 수 있는 "이용 가능" 더미 객체 생성
            NemcAvailabilityInfo availableInfo = new NemcAvailabilityInfo();
            availableInfo.setAvailableLevel("AVAILABLE");
            availableInfo.setUsable(30); // 1개 사용 가능
            availableInfo.setTotal(30);  // 총 1개
            availableInfo.setHasUnavailableMessage(false);
            availableInfo.setUnavailableMessages(new ArrayList<>());

            // 2. 증상 규칙(scoringRules)에 정의된 모든 필수 코드를 "이용 가능"으로 설정
            NemcElementMap rltmEmerCdMap = createElementMapFromRules(scoringRules.getRltmEmerCd(), availableInfo);
            NemcElementMap rltmCdMap = createElementMapFromRules(scoringRules.getRltmCd(), availableInfo);
            NemcElementMap svdssCdMap = createElementMapFromRules(scoringRules.getSvdssCd(), availableInfo);
            NemcElementMap rltmMeCdMap = createElementMapFromRules(scoringRules.getRltmMeCd(), availableInfo);

            // 3. (예외 처리) 병상 점수(calculateBedScore)를 위한 "입원실(O038)" 코드 강제 추가
            if (!rltmCdMap.getElements().containsKey("O038")) {
                NemcAvailabilityInfo bedInfo = new NemcAvailabilityInfo();
                bedInfo.setAvailableLevel("AVAILABLE");
                bedInfo.setUsable(5);// 5개 병상 사용 가능으로 설정
                bedInfo.setTotal(5);
                rltmCdMap.getElements().put("O038", bedInfo);
            }

            // 4. subTypes 리스트 생성
            List<String> subTypesList = scoringRules.getSubTypes().stream()
                    .map(SymptomRuleConfig.ScoringRuleItem::getCode)
                    .collect(Collectors.toList());

            return NemcHospitalData.builder()
                    .hospitalCode(hospital.getCode())
                    .name(hospital.getName())
                    .address(hospital.getAddress())
                    .distance(100.0) // 100m (테스트용 근접 거리)
                    .typeCode(hospital.getHospitalTypeCode().getCode())
                    .subTypes(subTypesList)
                    .rltmEmerCd(rltmEmerCdMap)
                    .rltmCd(rltmCdMap)
                    .svdssCd(svdssCdMap)
                    .rltmMeCd(rltmMeCdMap)
                    .erMessages(new ArrayList<>())
                    .unavailableMessages(new ArrayList<>())
                    .build();
        }
    }

    // "rltmEmerCd", "rltmCd" 등 공통 맵 구조
    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcElementMap {
        private boolean hasElement;
        private boolean isBlocked;
        private Map<String, NemcAvailabilityInfo> elements;
    }

    // "elements" 내부의 공통 가용성 정보
    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcAvailabilityInfo {
        private String availableLevel;
        private Integer total;
        private Integer usable;
        private boolean hasUnavailableMessage;
        private List<NemcUnavailableMessage> unavailableMessages;
    }

    // "erMessages"용 메시지 DTO
    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcMessage {
        private String message;
        private String category;
        private String reason;
        private String detail;
    }

    // "unavailableMessages"용 메시지 DTO
    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NemcUnavailableMessage {
        private String message;
        private String code;
        private String category;
        private String reason;
        private String detail;
    }
}