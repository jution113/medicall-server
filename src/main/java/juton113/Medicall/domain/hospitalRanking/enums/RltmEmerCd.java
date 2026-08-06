package juton113.Medicall.domain.hospitalRanking.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 실시간 응급실 병상 코드 (RLTM_EMER_CD)
public enum RltmEmerCd {
    EMERGENCY_GENERAL("O001", "응급실 일반"),
    EMERGENCY_PEDIATRIC("O002", "응급실 소아"),
    EMERGENCY_TRAUMA("O003", "응급실 외상"),
    EMERGENCY_CARDIO("O004", "응급실 심질환"),
    TRAUMA_RESUSCITATION("O048", "외상 소생실"),
    TRAUMA_SURGERY("O049", "외상 수술실"),
    EMERGENCY_NEGATIVE_PRESSURE("O059", "응급실 음압/격리"),
    EMERGENCY_GENERAL_ISOLATION("O060", "응급실 일반/격리");

    private final String code;
    private final String description;

    RltmEmerCd(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    // 코드값으로 Enum 상수를 찾는 정적 맵
    private static final Map<String, RltmEmerCd> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RltmEmerCd::getCode, Function.identity()));

    public static RltmEmerCd findByCode(String code) {
        return CODE_MAP.get(code);
    }
}