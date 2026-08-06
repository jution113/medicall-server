package juton113.Medicall.domain.incident.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import juton113.Medicall.domain.incident.dto.Vitals;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Converter
public class VitalsConverter implements AttributeConverter<Vitals, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Vitals vitalsObject) {
        if (vitalsObject == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(vitalsObject);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Vitals 객체를 JSON으로 변환 중 오류 발생", e);
        }
    }

    @Override
    public Vitals convertToEntityAttribute(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, Vitals.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON을 Vitals 객체로 변환 중 오류 발생", e);
        }
    }
}
