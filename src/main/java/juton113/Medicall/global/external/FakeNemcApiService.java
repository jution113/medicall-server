package juton113.Medicall.global.external;

import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalRepository;
import juton113.Medicall.domain.incident.dto.Location;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// k6 부하테스트 전용: 실제 NEMC API 호출 없이 로컬 DB에 시딩된 병원을 응답으로 돌려준다.
// 쿼리 파라미터(asort, rltmEmerCd 등)와 실제 위치 기반 반경 필터링은 무시한다.
// 대신 search_radius 값을 "반환할 병원 개수(N)"로 재해석해서 사용한다 — N=20/40/76 구간별
// 부하테스트를 DB 재시딩 없이 요청 파라미터만 바꿔서 재현하기 위한 의도적 설계.
@Profile("perf")
@Service
@RequiredArgsConstructor
public class FakeNemcApiService implements NemcApiService {

    private final HospitalRepository hospitalRepository;

    @Override
    public List<NemcHospitalData> getRealtimeHospitalData(
            String asort,
            String rltmEmerCd,
            String rltmCd,
            String svdssCd,
            String rltmMeCd,
            String subTypes,
            Location location,
            int radiusKm) {

        List<Hospital> hospitals = hospitalRepository.findAll().stream()
                .limit(radiusKm)
                .toList();
        List<NemcHospitalData> result = new ArrayList<>();

        for (Hospital hospital : hospitals) {
            result.add(NemcHospitalData.builder()
                    .hospitalCode(hospital.getCode())
                    .name(hospital.getName())
                    .address(hospital.getAddress())
                    .distance(1000.0)
                    .typeCode(hospital.getHospitalTypeCode().getCode())
                    .subTypes(new ArrayList<>())
                    .rltmEmerCd(emptyElementMap())
                    .rltmCd(emptyElementMap())
                    .svdssCd(emptyElementMap())
                    .rltmMeCd(emptyElementMap())
                    .erMessages(new ArrayList<>())
                    .unavailableMessages(new ArrayList<>())
                    .build());
        }

        return result;
    }

    private NemcElementMap emptyElementMap() {
        NemcElementMap elementMap = new NemcElementMap();
        elementMap.setElements(new java.util.HashMap<>());
        elementMap.setHasElement(false);
        elementMap.setBlocked(false);
        return elementMap;
    }
}
