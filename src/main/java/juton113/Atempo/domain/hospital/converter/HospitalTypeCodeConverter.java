package juton113.Atempo.domain.hospital.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import juton113.Atempo.domain.hospital.enums.HospitalTypeCode;

@Converter(autoApply = true) // 이 컨버터를 HospitalTypeCode에 자동으로 적용
public class HospitalTypeCodeConverter implements AttributeConverter<HospitalTypeCode, String> {

    // 1. Enum -> DB (Enum의 code 값을 DB에 저장)
    @Override
    public String convertToDatabaseColumn(HospitalTypeCode attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode(); // "A", "C", "D"를 반환
    }

    // 2. DB -> Enum (DB의 "A", "C", "D"를 Enum으로 변환)
    @Override
    public HospitalTypeCode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return HospitalTypeCode.findByCode(dbData); // "A" -> REGIONAL... Enum
    }
}