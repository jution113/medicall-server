package juton113.Atempo.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import juton113.Atempo.domain.inquiryLog.enums.RejectionReason;
import juton113.Atempo.global.auth.jwt.TokenService;
import juton113.Atempo.global.exception.CustomException;
import juton113.Atempo.global.external.aiGateway.dto.ConversationLog;
import juton113.Atempo.global.external.aiGateway.dto.InquiryMessageCreateRequest;
import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final TokenService tokenService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveRefreshToken(String key, String refreshToken) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, refreshToken, tokenService.getRefreshTokenTime(), TimeUnit.MILLISECONDS);
    }

    public void saveBlacklistedAccessToken(String key, String value) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        long expiration = tokenService.getExpiration(key);
        valueOperations.set(key, value, expiration, TimeUnit.MILLISECONDS);
    }

    public boolean validateBlacklistedToken(String accessToken) {
        return redisTemplate.hasKey(accessToken);
    }

    public void validateStoredToken(String key, String token) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        String foundToken = Optional.ofNullable(valueOperations.get(key)).orElseThrow(()-> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if(!foundToken.equals(token))
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public void deleteToken(String key) {
        if (!redisTemplate.delete(key))
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public void saveAiAnalysisResult(String callId, String value) {
        String key = "session:inquiry:analysis:" + callId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value, Duration.ofMinutes(5));
    }

    public String getAndDeleteAiAnalysisResult(String callId) {
        String key = "session:inquiry:analysis:" + callId;
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        return valueOperations.getAndDelete(key);
    }

    public void saveInqueryInfo(long sessionId, InquiryMessageCreateRequest inqueryInfo, Duration ttl) {
        try {
            String key = "session:inquiry:" + sessionId;
            String jsonData = objectMapper.writeValueAsString(inqueryInfo);
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

            if (ttl != null) {
                valueOperations.set(key, jsonData, ttl);
            } else {
                valueOperations.set(key, jsonData);
            }
        } catch (Exception e) {
            throw new RuntimeException("InqueryInfo 직렬화 실패", e);
        }
    }

    public void saveConversation(String callId, ConversationLog conversationLog, Duration ttl) {
        try {
            String key = "session:context:" + callId;
            String jsonData = objectMapper.writeValueAsString(conversationLog);
            ListOperations<String, String> listOperations = redisTemplate.opsForList();

            listOperations.rightPush(key, jsonData);

            if (ttl != null) {
                redisTemplate.expire(key, ttl);
            }
        } catch (Exception e) {
            throw new RuntimeException("InqueryInfo 직렬화 실패", e);
        }
    }

    public List<ConversationLog> getConversationLogs(String callId) {
        String key = "session:context:" + callId;
        ListOperations<String, String> listOperations = redisTemplate.opsForList();

        List<String> jsonLogs = listOperations.range(key, 0, -1);

        if (jsonLogs == null || jsonLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return jsonLogs.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, ConversationLog.class);
                    } catch (Exception e) {
                        throw new RuntimeException("ConversationLog 역직렬화 실패", e);
                    }
                })
                .collect(Collectors.toList());
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CooldownData {
        private RejectionReason reason;
        private LocalDateTime appliedAt;
    }

    public CooldownData getActiveCooldownInfo(String hospitalCode, String symptomKeyword) {
        String key = getCooldownKey(hospitalCode, symptomKeyword);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String jsonData = valueOperations.get(key);

        if (jsonData == null) {
            return null; // 쿨다운 없음
        }

        try {
            // JSON 문자열을 CooldownData 객체로 역직렬화
            return objectMapper.readValue(jsonData, CooldownData.class);
        } catch (Exception e) {
            // JSON 파싱 실패 시 (데이터 오염), 해당 키 삭제 및 null 반환
            redisTemplate.delete(key);
            return null;
        }
    }

    public void addRejectionCooldown(String hospitalCode, String symptomKeyword, RejectionReason reason, Duration ttl) {
        String key = getCooldownKey(hospitalCode, symptomKeyword);
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();

        // Value에 저장할 객체 생성
        CooldownData cooldownData = new CooldownData(reason, LocalDateTime.now());

        try {
            // 객체를 JSON 문자열로 직렬화
            String jsonData = objectMapper.writeValueAsString(cooldownData);
            valueOperations.set(key, jsonData, ttl);
        } catch (Exception e) {
            // 직렬화 실패 시 (로깅 필요)
            throw new RuntimeException("Redis CooldownData 직렬화 실패", e);
        }
    }

    private String getCooldownKey(String hospitalCode, String symptomKeyword) {
        return "rejection_history:" + hospitalCode + ":" + symptomKeyword;
    }

    private String getHistoryListKey(String hospitalCode) {
        return "rejection_history:" + hospitalCode;
    }

}
