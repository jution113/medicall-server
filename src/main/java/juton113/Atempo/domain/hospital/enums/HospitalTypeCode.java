package juton113.Atempo.domain.hospital.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HospitalTypeCode {
    REGIONAL_EMERGENCY_MEDICAL_CENTER("A", "권역응급의료센터"),
    LOCAL_EMERGENCY_MEDICAL_CENTER("C", "지역응급의료센터"),
    LOCAL_EMERGENCY_MEDICAL_INSTITUTION("D", "지역응급의료기관"),
    UNKNOWN("Z", "알 수 없음"); // 예외 처리용

    private final String code;
    private final String description;

    HospitalTypeCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    private static final Map<String, HospitalTypeCode> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(HospitalTypeCode::getCode, Function.identity()));

    public static HospitalTypeCode findByCode(String code) {
        return CODE_MAP.getOrDefault(code, UNKNOWN);
    }

    public static HospitalTypeCode findByApiCode(String apiCode) {
        if (apiCode == null) {
            return UNKNOWN;
        }

        // 서울시 응급실 API(G006, G007) 맵핑
        switch (apiCode) {
            // (API 명세나 JSON에서 '권역' 코드를 확인하여 G001 등으로 수정 필요)
            case "G001": // 예: 권역응급의료센터
                return REGIONAL_EMERGENCY_MEDICAL_CENTER; // "A"

            case "G006": // 지역응급의료센터
                return LOCAL_EMERGENCY_MEDICAL_CENTER;    // "C"

            case "G007": // 지역응급의료기관
                return LOCAL_EMERGENCY_MEDICAL_INSTITUTION; // "D"

            default:
                return UNKNOWN; // G099 (응급기관 이외) 등은 UNKNOWN 처리
        }
    }
}