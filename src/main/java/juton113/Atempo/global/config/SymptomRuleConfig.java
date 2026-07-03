package juton113.Atempo.global.config;

import juton113.Atempo.domain.incident.enums.SymptomKeyword;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class SymptomRuleConfig {

    private List<SymptomRule> rules = new ArrayList<>();

    public SymptomRule getRule(SymptomKeyword symptomKeyword) {
        return rules.stream()
                .filter(rule -> rule.getSymptomKeyword().equals(symptomKeyword.getKeyword()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("정의되지 않은 증상 키워드입니다: " + symptomKeyword));
    }

    @Getter
    @Setter
    public static class SymptomRule {
        private String symptomKeyword;
        private String description;
        private ApiFilters apiFilters;
        private ScoringRuleMap scoringRules;
    }

    @Getter
    @Setter
    public static class ApiFilters {
        private List<String> typeCode = new ArrayList<>();
        private List<String> subTypes = new ArrayList<>();
        private List<String> rltmEmerCd = new ArrayList<>();
        private List<String> rltmMeCd = new ArrayList<>();
        private List<String> svdssCd = new ArrayList<>();
        private List<String> rltmCd = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ScoringRuleMap {
        private List<ScoringRuleItem> rltmEmerCd = new ArrayList<>();
        private List<ScoringRuleItem> rltmCd = new ArrayList<>();
        private List<ScoringRuleItem> svdssCd = new ArrayList<>();
        private List<ScoringRuleItem> rltmMeCd = new ArrayList<>();
        private List<ScoringRuleItem> subTypes = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ScoringRuleItem {
        private String code;
        private int points;
    }
}