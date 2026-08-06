package juton113.Medicall.global.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String fileName = createFileName(file.getOriginalFilename());

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("S3 파일 업로드 성공: {}", fileName);

            return s3Client.utilities().getUrl(GetUrlRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
                            .build())
                    .toString();

        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드 중 에러가 발생했습니다.");
        }
    }

    private String createFileName(String originalFileName) {
        return UUID.randomUUID().toString().substring(0, 8) + "_" + originalFileName;
    }
}