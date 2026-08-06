package juton113.Medicall.global.exception;

import juton113.Medicall.global.util.enums.ErrorCode;
import lombok.Getter;

public class CustomException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage()); // logging setting
        this.errorCode = errorCode;
    }
}
