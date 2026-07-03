package juton113.Atempo.domain.incident.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SymptomKeyword {

    CARDIAC_ARREST("cardiac_arrest", "심정지 (비외상성, KTAS 1)"),
    NEURO_STROKE("neuro_stroke", "급성 뇌졸중 의증 (KTAS 1-2)"),
    AMI_CHEST_PAIN("ami_chest_pain", "급성 심근경색 의증 (KTAS 1-2)"),
    SEVERE_TRAUMA("severe_trauma", "중증외상 (KTAS 1-2)"),
    ANAPHYLAXIS("anaphylaxis", "아나필락시스 (KTAS 1-2)"),
    SEPTIC_SHOCK("septic_shock", "패혈증성 쇼크 (KTAS 1-2)"),
    SEIZURE_ACTIVE("seizure_active", "경련 (진행 중, KTAS 2-3)"),
    HYPOGLYCEMIA_COMA("hypoglycemia_coma", "저혈당 의식저하 (KTAS 2)"),
    BURN_SEVERE("burn_severe", "중증화상 (흡입손상 의심, KTAS 1-2)"),
    DELIVERY_EMERGENCY("delivery_emergency", "응급분만 (태아 만출 임박, KTAS 2)"),
    GASTRO_BLEEDING("gastro_bleeding", "대량 토혈/혈변 (쇼크 징후, KTAS 1-2)"),
    PEDIATRIC_ARREST("pediatric_arrest", "소아 심정지 (KTAS 1)");

    private final String keyword;
    private final String description;

    SymptomKeyword(String keyword, String description) {
        this.keyword = keyword;
        this.description = description;
    }

    @JsonValue
    public String getKeyword() {
        return keyword;
    }

    @JsonCreator
    public static SymptomKeyword fromKeyword(String keyword) {
        return Arrays.stream(values())
                .filter(symptom -> symptom.getKeyword().equals(keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 증상 키워드입니다: " + keyword));
    }
}
