package juton113.Medicall.domain.hospitalRanking.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 중증질환 수용 가능 여부 코드 (SVDSS_CD)
public enum SvdssCd {
    DISEASE_AMI_REPERFUSION("Y0010", "심근경색(재관류)"),
    DISEASE_STROKE_REPERFUSION("Y0020", "뇌경색(재관류)"),
    DISEASE_STROKE_SUBARACHNOID("Y0031", "뇌출혈수술(거미막하)"),
    DISEASE_STROKE_OTHER("Y0032", "뇌출혈수술(거미막하 외)"),
    DISEASE_AORTIC_THORACIC("Y0041", "대동맥응급(흉부)"),
    DISEASE_AORTIC_ABDOMINAL("Y0042", "대동맥응급(복부)"),
    DISEASE_BILIARY("Y0051", "담낭담관질환(담낭)"),
    DISEASE_BILIARY_PANCREAS("Y0052", "담낭담관질환(담관/췌장)"),
    DISEASE_GI_BLEEDING("Y0060", "위장관출혈"),
    DISEASE_INTUSSUSCEPTION_INFANT("Y0070", "장중첩/폐색(유아)"),
    DISEASE_GI_ENDOSCOPY_ADULT("Y0081", "위장관 응급내시경(성인)"),
    DISEASE_GI_ENDOSCOPY_INFANT("Y0082", "위장관 응급내시경(영유아)"),
    DISEASE_BRONCHOSCOPY_ADULT("Y0091", "기관지 응급내시경(성인)"),
    DISEASE_BRONCHOSCOPY_INFANT("Y0092", "기관지 응급내시경(영유아)"),
    DISEASE_NEONATAL_CARE("Y0100", "신생아 집중치료"),
    DISEASE_OBSTETRICS_DELIVERY("Y0111", "산부인과응급(분만)"),
    DISEASE_OBSTETRICS_SURGERY("Y0112", "산부인과응급(산과수술)"),
    DISEASE_GYNECOLOGY_SURGERY("Y0113", "산부인과응급(부인과수술)"),
    DISEASE_SEVERE_BURN("Y0120", "중증화상"),
    DISEASE_REPLANTATION_DIGIT("Y0131", "사지접합(수족지)"),
    DISEASE_REPLANTATION_OTHER("Y0132", "사지접합(수족지 외)"),
    DISEASE_TRAUMA_FACIAL("Y0141", "외상(안면)"),
    DISEASE_TRAUMA_OTHER("Y0142", "외상(안면 외)"),
    DISEASE_PSYCHIATRIC_INPATIENT("Y0150", "정신과적 응급입원"),
    DISEASE_OPHTHALMOLOGY_SURGERY("Y0160", "안과적 응급수술"),
    DISEASE_RADIOLOGY_ADULT("Y0171", "방사선중재술(성인)"),
    DISEASE_RADIOLOGY_INFANT("Y0172", "방사선중재술(영유아)");


    private final String code;
    private final String description;

    SvdssCd(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    // 코드값으로 Enum 상수를 찾는 정적 맵
    private static final Map<String, SvdssCd> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(SvdssCd::getCode, Function.identity()));

    public static SvdssCd findByCode(String code) {
        return CODE_MAP.get(code);
    }
}