package juton113.Medicall.global.external;

import juton113.Medicall.global.external.dto.EtaResult;

import java.math.BigDecimal;

public interface KakaoMobilityService {
    EtaResult getEtaAndDistance(BigDecimal originLon, BigDecimal originLat, BigDecimal destinationLon, BigDecimal destinationLat);
}
