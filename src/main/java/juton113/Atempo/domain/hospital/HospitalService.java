package juton113.Atempo.domain.hospital;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import juton113.Atempo.domain.hospital.enums.HospitalTypeCode;
import juton113.Atempo.domain.incident.dto.Location;
import juton113.Atempo.global.exception.CustomException;
import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;


import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final RestTemplate restTemplate; // 1. RestTemplate 주입

    @Value("${seoul.api.key}")
    private String SEOUL_API_KEY;

    private static final String SEOUL_API_BASE_URL = "http://openapi.seoul.go.kr:8088";
    private static final String HOSPITAL_INFO_SERVICE = "TvEmgcHospitalInfo"; // 응급실 정보 서비스
    private static final int PAGE_SIZE = 1000;
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    public Optional<Hospital> findHospital(String hospitalCode) {
        return hospitalRepository.findByCode(hospitalCode);
    }

    @Transactional
    public void fetchAndSynchronizeHospitalData() {
        log.info("서울시 병원 정보 동기화를 시작합니다.");

        // 1. 첫 페이지를 호출하여 총 데이터 수(totalCount) 조회
        SeoulHospitalApiResponse firstResponse = callSeoulApi(HOSPITAL_INFO_SERVICE, 1, 1);

        if (firstResponse == null || firstResponse.getHospitalInfo() == null || firstResponse.getHospitalInfo().getResult() == null) {
            throw new RuntimeException("서울시 API 첫 응답이 비어있습니다.");
        }
        if (!"INFO-000".equals(firstResponse.getHospitalInfo().getResult().getCode())) {
            throw new RuntimeException("서울시 API 호출 실패: " + firstResponse.getHospitalInfo().getResult().getMessage());
        }

        int totalCount = firstResponse.getHospitalInfo().getListTotalCount();
        log.info("총 {}개의 병원 정보를 확인했습니다.", totalCount);

        // 2. totalCount만큼, 1000개(PAGE_SIZE) 단위로 반복 호출
        for (int i = 0; i * PAGE_SIZE < totalCount; i++) {
            int startIndex = (i * PAGE_SIZE) + 1;
            int endIndex = (i + 1) * PAGE_SIZE;
            if (endIndex > totalCount) {
                endIndex = totalCount;
            }

            log.info("병원 정보 {} 부터 {} 까지 처리 중...", startIndex, endIndex);
            SeoulHospitalApiResponse response = callSeoulApi(HOSPITAL_INFO_SERVICE, startIndex, endIndex);

            if (response != null && response.getHospitalInfo() != null && response.getHospitalInfo().getRows() != null) {
                for (HospitalRowDto dto : response.getHospitalInfo().getRows()) {
                    saveOrUpdateHospital(dto);
                }
            }
        }
        log.info("병원 정보 동기화 완료.");
    }

    private void saveOrUpdateHospital(HospitalRowDto dto) {
        if (dto.getHpid() == null || dto.getHpid().isEmpty()) {
            return;
        }

        Optional<Hospital> optionalHospital = hospitalRepository.findByCode(dto.getHpid());

        Location location = (dto.getWgs84Lat() != null && dto.getWgs84Lon() != null) ?
                new Location(dto.getWgs84Lat(), dto.getWgs84Lon()) : null;

        LocalDateTime apiDataGeneratedAt = null;
        try {
            apiDataGeneratedAt = LocalDateTime.parse(dto.getWorkDttm(), API_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("WORK_DTTM 파싱 실패 (HPID: {}): {}", dto.getHpid(), dto.getWorkDttm());
        }

        // --- 2. 매핑 수정 ---
        String name = dto.getDutyName();
        HospitalTypeCode typeCode = HospitalTypeCode.findByApiCode(dto.getDutyEmcls());
        String address = dto.getDutyAddr();
        String phoneNumber = dto.getDutyTel3(); // 응급실 전화

        if (optionalHospital.isPresent()) {
            // 3-1. 기존 병원이 존재하면, 정보 업데이트
            Hospital hospital = optionalHospital.get();
            hospital.updateFromApi(name, typeCode, address, location, phoneNumber, apiDataGeneratedAt);
            hospitalRepository.save(hospital);
        } else {
            // 3-2. 기존 병원이 없으면, 새로 생성
            Hospital hospital = new Hospital();
            hospital.setCode(dto.getHpid()); // 고유 코드 설정
            hospital.updateFromApi(name, typeCode, address, location, phoneNumber, apiDataGeneratedAt);
            hospitalRepository.save(hospital);
        }
    }

    private SeoulHospitalApiResponse callSeoulApi(String serviceName, int startIndex, int endIndex) {

        // 1. URL 및 쿼리 파라미터 빌드 (주의: 서울 API는 Path에 파라미터를 넣습니다)
        URI uri = UriComponentsBuilder
                .fromUriString(SEOUL_API_BASE_URL)
                .path("/{key}/json/{serviceName}/{startIndex}/{endIndex}")
                .buildAndExpand(SEOUL_API_KEY, serviceName, startIndex, endIndex)
                .toUri();

        // 2. HTTP 헤더 X

        try {
            // 3. API 호출 (Kakao와 달리 헤더가 필요 없으므로 requestEntity = null)
            ResponseEntity<SeoulHospitalApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null, // requestEntity
                    SeoulHospitalApiResponse.class
            );

            // 4. 필요한 값 추출
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

        } catch (Exception e) {
            throw new RuntimeException("서울 API 호출 중 오류 발생: " + e.getMessage());
        }

        throw new RuntimeException("유효한 병원 정보를 가져오지 못했습니다.");
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeoulHospitalApiResponse {
        @JsonProperty("TvEmgcHospitalInfo")
        private HospitalInfoWrapper hospitalInfo;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HospitalInfoWrapper {
        @JsonProperty("list_total_count")
        private int listTotalCount;
        @JsonProperty("RESULT")
        private ApiResult result;
        @JsonProperty("row")
        private List<HospitalRowDto> rows;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResult {
        @JsonProperty("CODE")
        private String code;
        @JsonProperty("MESSAGE")
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HospitalRowDto {
        @JsonProperty("HPID")
        private String hpid; // 1. code

        @JsonProperty("DUTYEMCLS")
        private String dutyEmcls; // 2. hospitalTypeCode (전문 코드)

        @JsonProperty("DUTYADDR")
        private String dutyAddr; // 3. address

        @JsonProperty("WGS84LON")
        private BigDecimal wgs84Lon; // 4. location (경도)

        @JsonProperty("WGS84LAT")
        private BigDecimal wgs84Lat; // 4. location (위도)

        @JsonProperty("DUTYNAME")
        private String dutyName; // 5. name

        @JsonProperty("DUTYTEL3")
        private String dutyTel3; // 6. phoneNumber (응급실 전화)

        @JsonProperty("WORK_DTTM")
        private String workDttm; // 7. apiDataGeneratedAt
    }
}
