package juton113.Atempo.global.exception;

import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {
    @Getter
    private final ErrorCode errorCode;

    public JwtAuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
