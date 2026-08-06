package juton113.Medicall.domain.incident.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum KtasLevel {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5);

    private final int level;

    KtasLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    @JsonCreator
    public static KtasLevel fromLevel(int level) {
        for (KtasLevel ktas : values()) {
            if (ktas.level == level) {
                return ktas;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 KTAS 레벨입니다: " + level);
    }
}
