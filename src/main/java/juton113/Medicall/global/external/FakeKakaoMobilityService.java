package juton113.Medicall.global.external;

import juton113.Medicall.global.external.dto.EtaResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// k6 부하테스트 전용: 실제 카카오 모빌리티 API 호출 없이 고정값을 즉시 반환한다.
@Profile("perf")
@Service
public class FakeKakaoMobilityService implements KakaoMobilityService {

    @Override
    public EtaResult getEtaAndDistance(BigDecimal originLon, BigDecimal originLat, BigDecimal destinationLon, BigDecimal destinationLat) {
        return new EtaResult(300, 5000);
    }
}
