package juton113.Medicall.domain.hospitalRanking.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 실시간 의료 장비 코드 (RLTM_ME_CD)
public enum RltmMeCd {
    EQUIP_CT("O027", "CT"),
    EQUIP_MRI("O028", "MRI"),
    EQUIP_VENTILATOR("O029", "인공호흡기"),
    EQUIP_ANGIOGRAPHY("O030", "혈관촬영기 (Angiography)"),
    EQUIP_ENDOSCOPY("O031", "내시경"),
    EQUIP_CRRT("O032", "인공신장기 (CRRT)"),
    EQUIP_ECMO("O033", "체외순환기 (ECMO)"),
    EQUIP_ULTRASONOGRAPHY("O034", "초음파기"),
    EQUIP_HYPOTHERMIA("O035", "저체온치료기"),
    EQUIP_NIBP("O037", "NIBP (비침습혈압감시)");

    private final String code;
    private final String description;

    RltmMeCd(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    private static final Map<String, RltmMeCd> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RltmMeCd::getCode, Function.identity()));

    public static RltmMeCd findByCode(String code) {
        return CODE_MAP.get(code);
    }
}