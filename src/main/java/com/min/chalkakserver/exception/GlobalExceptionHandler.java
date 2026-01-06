package com.min.chalkakserver.exception;

import com.min.chalkakserver.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 네컷사진관을 찾을 수 없는 경우
     */
    @ExceptionHandler(PhotoBoothNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePhotoBoothNotFoundException(
            PhotoBoothNotFoundException ex, HttpServletRequest request) {
        
        log.error("PhotoBooth not found: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getPhotoBoothId() != null) {
            details.put("photoBoothId", ex.getPhotoBoothId());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 잘못된 위치 정보인 경우
     */
    @ExceptionHandler(InvalidLocationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLocationException(
            InvalidLocationException ex, HttpServletRequest request) {
        
        log.error("Invalid location: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getLatitude() != null) {
            details.put("latitude", ex.getLatitude());
        }
        if (ex.getLongitude() != null) {
            details.put("longitude", ex.getLongitude());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 중복된 네컷사진관인 경우
     */
    @ExceptionHandler(DuplicatePhotoBoothException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhotoBoothException(
            DuplicatePhotoBoothException ex, HttpServletRequest request) {
        
        log.error("Duplicate photo booth: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", ex.getName());
        details.put("address", ex.getAddress());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 인증 관련 예외
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            AuthException ex, HttpServletRequest request) {
        
        log.error("Authentication error: {} (code: {})", ex.getMessage(), ex.getCode());
        
        HttpStatus status;
        String error;
        
        switch (ex.getCode()) {
            case "CONFLICT":
                status = HttpStatus.CONFLICT;
                error = "Conflict";
                break;
            case "UNAUTHORIZED":
                status = HttpStatus.UNAUTHORIZED;
                error = "Unauthorized";
                break;
            case "NOT_FOUND":
                status = HttpStatus.NOT_FOUND;
                error = "Not Found";
                break;
            default:
                status = HttpStatus.UNAUTHORIZED;
                error = "Unauthorized";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            status.value(),
            error,
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * 리뷰를 찾을 수 없는 경우
     */
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFoundException(
            ReviewNotFoundException ex, HttpServletRequest request) {
        
        log.error("Review not found: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("reviewId", ex.getReviewId());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 중복 리뷰 작성 시도
     */
    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReviewException(
            DuplicateReviewException ex, HttpServletRequest request) {
        
        log.error("Duplicate review: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("photoBoothId", ex.getPhotoBoothId());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.error("Validation failed: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        details.put("fieldErrors", fieldErrors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "입력 데이터가 유효하지 않습니다.",
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 타입 변환 실패
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.error("Type mismatch: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("parameter", ex.getName());
        details.put("value", ex.getValue());
        details.put("requiredType", ex.getRequiredType() != null ? 
                ex.getRequiredType().getSimpleName() : "unknown");
        
        String message = String.format("파라미터 '%s'의 값 '%s'을(를) %s 타입으로 변환할 수 없습니다.",
                ex.getName(), ex.getValue(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "요구되는");
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI(),
            details
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 기타 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
