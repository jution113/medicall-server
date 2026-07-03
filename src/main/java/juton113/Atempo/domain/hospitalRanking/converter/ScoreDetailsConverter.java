package juton113.Atempo.domain.hospitalRanking.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import juton113.Atempo.domain.hospitalRanking.dto.ScoreDetails;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Converter
public class ScoreDetailsConverter implements AttributeConverter<ScoreDetails, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ScoreDetails scoreDetailsObject) {
        if (scoreDetailsObject == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(scoreDetailsObject);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("ScoreDetails 객체를 JSON으로 변환 중 오류 발생", e);
        }
    }

    @Override
    public ScoreDetails convertToEntityAttribute(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, ScoreDetails.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON을 ScoreDetails 객체로 변환 중 오류 발생", e);
        }
    }
}
