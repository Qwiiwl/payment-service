package uzumtech.paymentservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uzumtech.paymentservice.exception.*;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Единый формат ответа для всех ошибок
    public record ErrorResponse(
            String code,
            String message,
            int status,
            String path,
            LocalDateTime timestamp,
            List<FieldViolation> fieldErrors
    ) {
        public record FieldViolation(String field, String message) {}
    }


    // Ошибка транзакций
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex, HttpServletRequest req) {
        return buildResponse("CARD_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, req, null);
    }

    @ExceptionHandler(CardInactiveException.class)
    public ResponseEntity<ErrorResponse> handleCardInactive(CardInactiveException ex, HttpServletRequest req) {
        return buildResponse("CARD_INACTIVE", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return buildResponse("INSUFFICIENT_FUNDS", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    @ExceptionHandler(FineAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleFineAlreadyPaid(FineAlreadyPaidException ex, HttpServletRequest req) {
        return buildResponse("FINE_ALREADY_PAID", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    // отп

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex, HttpServletRequest req) {
        return buildResponse("INVALID_OTP", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpired(OtpExpiredException ex, HttpServletRequest req) {
        return buildResponse("OTP_EXPIRED", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExists(CardAlreadyExistsException ex, HttpServletRequest req) {
        return buildResponse("CARD_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT, req, null);
    }


    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest req) {
        return buildResponse("USER_ALREADY_EXISTS", ex.getMessage(), HttpStatus.CONFLICT, req, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return buildResponse("INVALID_CREDENTIALS", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    // Валидация DTO аннотаций + @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();
        return buildResponse("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST, req, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldViolation> fields = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return buildResponse("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST, req, fields);
    }

    // Некорректный JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return buildResponse("INVALID_REQUEST_BODY", "Malformed JSON request", HttpStatus.BAD_REQUEST, req, null);
    }

    // Любые IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return buildResponse("INVALID_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    // Общий фолбек
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        ex.printStackTrace(); //отладчик
        return buildResponse("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR, req, null);
    }

    // Унифицированный билдер ответа
    private ResponseEntity<ErrorResponse> buildResponse(
            String code,
            String message,
            HttpStatus status,
            HttpServletRequest req,
            List<ErrorResponse.FieldViolation> fieldErrors
    ) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        code,
                        message,
                        status.value(),
                        req.getRequestURI(),
                        LocalDateTime.now(),
                        fieldErrors
                ));
    }

    private ErrorResponse.FieldViolation toFieldViolation(FieldError fe) {
        return new ErrorResponse.FieldViolation(fe.getField(), fe.getDefaultMessage());
    }
}