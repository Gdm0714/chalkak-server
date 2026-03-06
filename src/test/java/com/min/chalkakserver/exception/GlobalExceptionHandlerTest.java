package com.min.chalkakserver.exception;

import com.min.chalkakserver.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    @DisplayName("PhotoBoothNotFoundException → 404, photoBoothId in details")
    void handlePhotoBoothNotFoundException_returns404WithPhotoBoothId() {
        // given
        request.setRequestURI("/api/photo-booths/99");
        PhotoBoothNotFoundException ex = new PhotoBoothNotFoundException(99L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handlePhotoBoothNotFoundException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getDetails()).containsKey("photoBoothId");
        assertThat(response.getBody().getDetails().get("photoBoothId")).isEqualTo(99L);
    }

    @Test
    @DisplayName("ConstraintViolationException → 400, fieldErrors in details")
    void handleConstraintViolationException_returns400WithFieldErrors() {
        // given
        request.setRequestURI("/api/photo-booths");
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        given(path.toString()).willReturn("name");
        given(violation.getPropertyPath()).willReturn(path);
        given(violation.getMessage()).willReturn("must not be blank");

        Set<ConstraintViolation<?>> violations = Set.of(violation);
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetails()).containsKey("fieldErrors");
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().getDetails().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("name", "must not be blank");
    }

    @Test
    @DisplayName("InvalidLocationException → 400, latitude/longitude in details")
    void handleInvalidLocationException_returns400WithCoordinates() {
        // given
        request.setRequestURI("/api/photo-booths/nearby");
        InvalidLocationException ex = new InvalidLocationException(200.0, 200.0);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleInvalidLocationException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetails()).containsKey("latitude");
        assertThat(response.getBody().getDetails()).containsKey("longitude");
        assertThat(response.getBody().getDetails().get("latitude")).isEqualTo(200.0);
        assertThat(response.getBody().getDetails().get("longitude")).isEqualTo(200.0);
    }

    @Test
    @DisplayName("DuplicatePhotoBoothException → 409, name/address in details")
    void handleDuplicatePhotoBoothException_returns409WithNameAndAddress() {
        // given
        request.setRequestURI("/api/admin/photo-booths");
        DuplicatePhotoBoothException ex = new DuplicatePhotoBoothException("하루필름", "서울시 강남구");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleDuplicatePhotoBoothException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getDetails()).containsEntry("name", "하루필름");
        assertThat(response.getBody().getDetails()).containsEntry("address", "서울시 강남구");
    }

    @Test
    @DisplayName("AuthException(default code) → 401")
    void handleAuthException_defaultCode_returns401() {
        // given
        request.setRequestURI("/api/auth/me");
        AuthException ex = new AuthException("Token expired");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAuthException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Token expired");
    }

    @Test
    @DisplayName("AuthException(CONFLICT) → 409")
    void handleAuthException_conflictCode_returns409() {
        // given
        request.setRequestURI("/api/auth/register");
        AuthException ex = new AuthException("Email already exists", "CONFLICT");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAuthException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("AuthException(UNAUTHORIZED) → 401")
    void handleAuthException_unauthorizedCode_returns401() {
        // given
        request.setRequestURI("/api/auth/me");
        AuthException ex = new AuthException("Invalid token", "UNAUTHORIZED");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAuthException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("AuthException(NOT_FOUND) → 404")
    void handleAuthException_notFoundCode_returns404() {
        // given
        request.setRequestURI("/api/auth/me");
        AuthException ex = new AuthException("User not found", "NOT_FOUND");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleAuthException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("ReviewNotFoundException → 404, reviewId in details")
    void handleReviewNotFoundException_returns404WithReviewId() {
        // given
        request.setRequestURI("/api/reviews/42");
        ReviewNotFoundException ex = new ReviewNotFoundException(42L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleReviewNotFoundException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getDetails()).containsEntry("reviewId", 42L);
    }

    @Test
    @DisplayName("DuplicateReviewException → 409, photoBoothId in details")
    void handleDuplicateReviewException_returns409WithPhotoBoothId() {
        // given
        request.setRequestURI("/api/reviews/photo-booth/5");
        DuplicateReviewException ex = new DuplicateReviewException(5L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateReviewException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getDetails()).containsEntry("photoBoothId", 5L);
    }

    @Test
    @DisplayName("DuplicateCongestionReportException → 409, photoBoothId in details")
    void handleDuplicateCongestionReportException_returns409WithPhotoBoothId() {
        // given
        request.setRequestURI("/api/congestion/photo-booth/7");
        DuplicateCongestionReportException ex = new DuplicateCongestionReportException(7L);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateCongestionReportException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getDetails()).containsEntry("photoBoothId", 7L);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400, fieldErrors in details")
    void handleValidationException_returns400WithFieldErrors() {
        // given
        request.setRequestURI("/api/auth/register");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("emailRegisterRequestDto", "email", "must not be blank");
        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getAllErrors()).willReturn(List.of(fieldError));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetails()).containsKey("fieldErrors");
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrors = (Map<String, String>) response.getBody().getDetails().get("fieldErrors");
        assertThat(fieldErrors).containsEntry("email", "must not be blank");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400")
    void handleTypeMismatchException_returns400() {
        // given
        request.setRequestURI("/api/photo-booths/abc");
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        given(ex.getName()).willReturn("id");
        given(ex.getValue()).willReturn("abc");
        given(ex.getRequiredType()).willReturn((Class) Long.class);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatchException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetails()).containsKey("parameter");
        assertThat(response.getBody().getDetails().get("parameter")).isEqualTo("id");
    }

    @Test
    @DisplayName("DataIntegrityViolationException → 409")
    void handleDataIntegrityViolationException_returns409() {
        // given
        request.setRequestURI("/api/reviews/photo-booth/3");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("Exception (generic) → 500")
    void handleGenericException_returns500() {
        // given
        request.setRequestURI("/api/photo-booths");
        Exception ex = new RuntimeException("Unexpected error");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
    }

    @Test
    @DisplayName("PhotoBoothNotFoundException(message only) → 404, details null-safe")
    void handlePhotoBoothNotFoundException_messageOnly_returns404WithNullDetails() {
        // given
        request.setRequestURI("/api/photo-booths");
        PhotoBoothNotFoundException ex = new PhotoBoothNotFoundException("사진관 없음");

        // when
        ResponseEntity<ErrorResponse> response = handler.handlePhotoBoothNotFoundException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        // details map should be empty (photoBoothId is null so not added)
        assertThat(response.getBody().getDetails()).isEmpty();
    }

    @Test
    @DisplayName("InvalidLocationException(message only) → 400, no lat/lon in details")
    void handleInvalidLocationException_messageOnly_returns400WithEmptyDetails() {
        // given
        request.setRequestURI("/api/photo-booths/nearby");
        InvalidLocationException ex = new InvalidLocationException("Invalid location");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleInvalidLocationException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getDetails()).isEmpty();
    }
}
