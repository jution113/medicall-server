package juton113.Atempo.global.exception;

import juton113.Atempo.global.util.enums.ErrorCode;
import lombok.Getter;

public class CustomException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // logging setting
        this.errorCode = errorCode;
    }
}
