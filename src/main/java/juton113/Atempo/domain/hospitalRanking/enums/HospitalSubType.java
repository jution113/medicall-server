package juton113.Atempo.domain.hospitalRanking.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 병원 서브타입
@Getter
public enum HospitalSubType {
    CHILD("CHILD", "소아전문응급의료센터"),
    TRAUMA("TRAUMA", "권역외상센터"),
    SEVERE("SEVERE", "중증응급진료센터"),
    ISOLATION_BED("ISL_SICKBED", "국가지정 입원치료병상 운영");

    private final String code;
    private final String description;

    HospitalSubType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<String, HospitalSubType> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(HospitalSubType::getCode, Function.identity()));

    public static HospitalSubType findByCode(String code) {
        return CODE_MAP.get(code);
    }
}