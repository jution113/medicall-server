package juton113.Atempo.domain.hospital;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/hospitals")
public class HospitalController {
    private final HospitalService hospitalService;

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshHospitalData() {
        try {
            hospitalService.fetchAndSynchronizeHospitalData();
            return ResponseEntity.ok("병원 정보 동기화가 성공적으로 시작되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("동기화 실패: " + e.getMessage());
        }
    }
}
