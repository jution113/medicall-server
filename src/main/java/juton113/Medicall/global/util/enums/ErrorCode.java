package juton113.Medicall.global.util.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Member-related exceptions
    USER_NOT_FOUND("User not found.", HttpStatus.NOT_FOUND),

    // Session
    SESSION_NOT_FOUND("Session not found.", HttpStatus.NOT_FOUND),

    // ReviewLog
    REVIEW_NOT_FOUND("Review not found.", HttpStatus.NOT_FOUND),

    // Hospital-Number
    HOSPITAL_NUMBER_NOT_FOUND("Hospital Number not found.", HttpStatus.NOT_FOUND),

    // Token-related exceptions
    REFRESH_TOKEN_NOT_FOUND("Refresh token not found.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN_TYPE("Invalid token type.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("Token has expired.", HttpStatus.UNAUTHORIZED),
    INVALID_SIGNATURE("Invalid token signature.", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("Malformed token.", HttpStatus.UNAUTHORIZED),
    LOGOUT_TOKEN("Token has already been logged out.", HttpStatus.UNAUTHORIZED),

    // Authorization-related exceptions
    ACCESS_DENIED("Access denied.", HttpStatus.FORBIDDEN),

    // Header-related exceptions
    AUTH_HEADER_MISSING("Authorization header is missing.", HttpStatus.UNAUTHORIZED),
    INVALID_AUTH_HEADER_FORMAT("Invalid Authorization header format.", HttpStatus.UNAUTHORIZED),

    // HospitalCallStatus
    HOSPITAL_CALL_STATUS_NOT_FOUND("Hospital call status not found.", HttpStatus.NOT_FOUND),

    // Hospital
    HOSPITAL_NOT_FOUND("Hospital not found.", HttpStatus.NOT_FOUND),

    // HospitalRanking
    HOSPITAL_RANKING_NOT_FOUND("Hospital Ranking not found.", HttpStatus.NOT_FOUND),

    // Inquiry Log
    INQUIRY_LOG_NOT_FOUND("Inquiry Log not found.", HttpStatus.NOT_FOUND),

    // ML server-related exceptions
    ML_SERVER_ERROR("An error occurred while communicating with the ML server.", HttpStatus.INTERNAL_SERVER_ERROR),
    ML_BAD_REQUEST("Invalid request sent to the ML server.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
