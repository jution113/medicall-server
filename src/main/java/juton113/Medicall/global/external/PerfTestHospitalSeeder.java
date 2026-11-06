package juton113.Medicall.global.external;

import juton113.Medicall.domain.hospital.Hospital;
import juton113.Medicall.domain.hospital.HospitalRepository;
import juton113.Medicall.domain.hospital.enums.HospitalTypeCode;
import juton113.Medicall.domain.incident.dto.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// k6 부하테스트 전용: FakeNemcApiService가 hospitalRepository.findAll()에서 잘라 쓸 병원 풀을 시딩한다.
// N=20/40/76 구간별 부하테스트를 재시딩 없이 재현하려면, 여기서 시딩하는 총량이 가장 큰 N(76) 이상이어야 한다.
// 기동 시마다 실행되므로 이미 시딩된 코드는 건너뛰어 재실행해도 중복 삽입되지 않는다.
@Slf4j
@Component
@Profile("perf")
@RequiredArgsConstructor
public class PerfTestHospitalSeeder implements CommandLineRunner {

    private static final HospitalTypeCode[] TYPE_CYCLE = {
            HospitalTypeCode.REGIONAL_EMERGENCY_MEDICAL_CENTER,
            HospitalTypeCode.LOCAL_EMERGENCY_MEDICAL_CENTER,
            HospitalTypeCode.LOCAL_EMERGENCY_MEDICAL_INSTITUTION,
    };

    private static final BigDecimal BASE_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal BASE_LONGITUDE = new BigDecimal("126.9780");
    private static final BigDecimal STEP = new BigDecimal("0.001");

    private final HospitalRepository hospitalRepository;

    @Value("${perf.seed.hospital-count:20}")
    private int hospitalCount;

    @Override
    public void run(String... args) {
        int seeded = 0;
        for (int i = 0; i < hospitalCount; i++) {
            String code = seedCode(i);
            if (hospitalRepository.findByCode(code).isPresent()) {
                continue;
            }
            hospitalRepository.save(buildHospital(code, i));
            seeded++;
        }
        log.info("perf 프로필 병원 시딩 완료: 총 {}건 중 {}건 신규 생성", hospitalCount, seeded);
    }

    private String seedCode(int index) {
        return String.format("P%07d", index);
    }

    private Hospital buildHospital(String code, int index) {
        Hospital hospital = new Hospital();
        hospital.setCode(code);
        hospital.setName("perf-hospital-" + index);
        hospital.setHospitalTypeCode(TYPE_CYCLE[index % TYPE_CYCLE.length]);
        hospital.setAddress("서울시 perf 테스트용 주소 " + index);
        hospital.setLocation(Location.of(
                BASE_LATITUDE.add(STEP.multiply(BigDecimal.valueOf(index))),
                BASE_LONGITUDE.add(STEP.multiply(BigDecimal.valueOf(index)))
        ));
        hospital.setPhoneNumber("010-0000-0000");
        hospital.setApiDataGeneratedAt(LocalDateTime.now());
        return hospital;
    }
}
