package com.entec.tax.common.exception;

import com.entec.tax.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * - @RestControllerAdvice를 통한 전역 예외 처리
 * - BusinessException, Validation 예외, 시스템 예외 등을 일관된 ApiResponse 형태로 변환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * BusinessException 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        String message = e.getDetailMessage() != null ? e.getDetailMessage() : errorCode.getMessage();

        log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), message);

        ApiResponse<Void> response = ApiResponse.error(errorCode.getHttpStatus(), message);
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getHttpStatus()));
    }

    /**
     * @Valid 유효성 검증 실패 처리 (RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[ValidationException] {}", errorMessage);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_PARAMETER.getHttpStatus(),
                errorMessage
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * @Valid 유효성 검증 실패 처리 (ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[BindException] {}", errorMessage);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_PARAMETER.getHttpStatus(),
                errorMessage
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 필수 파라미터 누락 처리
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "필수 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다.";
        log.warn("[MissingParameter] {}", message);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BAD_REQUEST.getHttpStatus(), message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 파라미터 타입 불일치 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "파라미터 '" + e.getName() + "'의 타입이 올바르지 않습니다.";
        log.warn("[TypeMismatch] {}", message);

        ApiResponse<Void> response = ApiResponse.error(ErrorCode.BAD_REQUEST.getHttpStatus(), message);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * HTTP 메소드 불일치 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotAllowed] {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus(),
                ErrorCode.METHOD_NOT_ALLOWED.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * 핸들러 없음 (404) 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        log.warn("[NotFound] {}", e.getRequestURL());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.NOT_FOUND.getHttpStatus(),
                ErrorCode.NOT_FOUND.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 파일 크기 초과 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("[MaxUploadSize] {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.FILE_SIZE_EXCEEDED.getHttpStatus(),
                ErrorCode.FILE_SIZE_EXCEEDED.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 데이터 무결성 위반 처리
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("[DataIntegrity] {}", e.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.DATA_INTEGRITY_ERROR.getHttpStatus(),
                ErrorCode.DATA_INTEGRITY_ERROR.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[UnhandledException] ", e);

        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
