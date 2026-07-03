package juton113.Atempo.domain.hospitalRanking.enums;

import lombok.Getter;

@Getter
public enum KtasLevelWeight {

    LEVEL_1(0.1, 0.9) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            return (protocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    },
    LEVEL_2(0.7, 0.3) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            return (protocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    },
    LEVEL_3(0.5, 0.5) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            return (protocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    },
    LEVEL_4_5(0.5, 0.5) {
        @Override
        public double calculateBaseScore(double protocolScore, double timeScore) {
            double inverseProtocolScore = 100.0 - protocolScore;

            return (inverseProtocolScore * this.getProtocolWeight()) + (timeScore * this.getTimeWeight());
        }
    };

    private final double protocolWeight;
    private final double timeWeight;

    KtasLevelWeight(double protocolWeight, double timeWeight) {
        this.protocolWeight = protocolWeight;
        this.timeWeight = timeWeight;
    }

    public static KtasLevelWeight fromLevel(int level) {
        return switch (level) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            case 4, 5 -> LEVEL_4_5;
            default -> throw new IllegalArgumentException("유효하지 않은 KTAS 레벨입니다: " + level);
        };
    }

    public abstract double calculateBaseScore(double protocolScore, double timeScore);
}