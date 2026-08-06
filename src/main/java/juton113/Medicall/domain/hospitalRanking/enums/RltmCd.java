package juton113.Medicall.domain.hospitalRanking.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 실시간 진료과별 병상 코드 (RLTM_CD)
public enum RltmCd {
    ICU_GENERAL("O005", "일반 중환자실"),
    ICU_EMERGENCY("O006", "응급 중환자실"),
    ICU_TRAUMA("O007", "외상 중환자실"),
    ICU_NEUROSURGERY("O008", "신경외과 중환자실"),
    ICU_NEUROLOGY("O009", "신경과 중환자실"),
    ICU_PEDIATRIC("O010", "소아 중환자실"),
    ICU_CARDIO_MEDICINE("O011", "심장내과 중환자실"),
    ICU_CARDIO_SURGERY("O012", "흉부외과 중환자실"),
    ICU_BURN("O013", "화상 중환자실"),
    ICU_NEONATAL("O014", "신생아 중환자실 (NICU)"),
    ICU_INTERNAL_MEDICINE("O015", "내과 중환자실"),
    ICU_SURGERY("O016", "외과 중환자실"),
    ICU_NEUROLOGY_STROKE("O017", "신경과 뇌졸중"), // 뇌졸중 집중치료실(SU)
    ICU_INTERNAL_PNEUMO("O018", "내과 호흡기"),
    ICU_PEDIATRIC_EMERGENCY("O019", "소아 응급"),
    DELIVERY_ROOM("O021", "분만실"),
    SURGERY_ROOM("O022", "수술실"),
    BURN_TREATMENT_ROOM("O023", "화상 처치실"),
    TRAUMA_BAY("O024", "외상 병실"),
    TRAUMA_SURGERY_ROOM("O025", "외상 수술실"),
    TRAUMA_ICU("O026", "외상 중환자실"),
    PEDIATRIC_WARD("O036", "소아 입원실"),
    WARD_GENERAL("O038", "입원실 (일반)"), // S_bed 점수 계산 대상
    WARD_NEGATIVE_PRESSURE("O050", "입원실 음압"),
    WARD_GENERAL_ISOLATION("O051", "입원실 일반격리"),
    WARD_PEDIATRIC_ISOLATION("O052", "소아 일반격리"),
    WARD_PEDIATRIC_NEGATIVE_PRESSURE("O053", "소아 음압격리");


    private final String code;
    private final String description;

    RltmCd(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    private static final Map<String, RltmCd> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RltmCd::getCode, Function.identity()));

    public static RltmCd findByCode(String code) {
        return CODE_MAP.get(code);
    }
}