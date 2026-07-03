package juton113.Atempo.domain.inquiryLog.enums;

import lombok.Getter;

import java.time.Duration;
import java.util.Arrays;

@Getter
public enum RejectionReason {
    BED_SHORTAGE(Duration.ofHours(1), "병상 부족"),
    EQUIPMENT_SHORTAGE(Duration.ofHours(2), "장비 부족"),
    STAFF_SHORTAGE(Duration.ofHours(4), "의료진 부족"),
    OTHER(Duration.ofMinutes(30), "기타 사유");

    private final Duration cooldownDuration;
    private final String description;

    RejectionReason(Duration cooldownDuration, String description) {
        this.cooldownDuration = cooldownDuration;
        this.description = description;
    }

    public static RejectionReason fromString(String text) {
        return Arrays.stream(RejectionReason.values())
                .filter(reason -> reason.name().equalsIgnoreCase(text) || reason.description.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 거절 사유입니다: " + text));
    }
}
