package com.entec.tax.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 전역 예외 처리기
 * <p>
 * 모든 컨트롤러에서 발생하는 예외를 포착하여
 * Section 28.5 에러 응답 스키마에 맞는 표준 JSON 을 반환한다.
 * <p>
 * 에러 발생 시 LOG_CALCULATION 로거를 통해 계산 로그에도 기록한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 계산 관련 에러를 별도 로거로 기록하기 위한 로거.
     * logback 설정에서 LOG_CALCULATION appender 로 라우팅된다.
     */
    private static final Logger LOG_CALCULATION = LoggerFactory.getLogger("LOG_CALCULATION");

    // ─── ValidationException ──────────────────────────────────────────

    /**
     * 입력 값 검증 실패 예외 처리
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (ValidationException.FieldError fe : ex.getFieldErrors()) {
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("field", fe.getField());
            detail.put("issue", fe.getIssue());
            detail.put("expected", fe.getExpected());
            detail.put("received", fe.getReceived());
            details.add(detail);
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getDetailMessage())
                .details(details)
                .reqId(ex.getReqId())
                .traceId(traceId)
                .build();

        logError(ex, traceId);

        return new ResponseEntity<ErrorResponse>(response, ex.getErrorCode().getHttpStatus());
    }

    // ─── CalculationException ─────────────────────────────────────────

    /**
     * 세액 계산 단계 오류 처리
     */
    @ExceptionHandler(CalculationException.class)
    public ResponseEntity<ErrorResponse> handleCalculationException(
            CalculationException ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("calc_step", ex.getCalcStep());
        detail.put("description", ex.getDetailMessage());
        details.add(detail);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getDetailMessage())
                .details(details)
                .reqId(ex.getReqId())
                .traceId(traceId)
                .build();

        logError(ex, traceId);

        return new ResponseEntity<ErrorResponse>(response, ex.getErrorCode().getHttpStatus());
    }

    // ─── HardFailException ────────────────────────────────────────────

    /**
     * 결산조정 차단(Hard-Fail) 오류 처리
     */
    @ExceptionHandler(HardFailException.class)
    public ResponseEntity<ErrorResponse> handleHardFailException(
            HardFailException ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (String item : ex.getBlockedItems()) {
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("blocked_item", item);
            details.add(detail);
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getDetailMessage())
                .details(details)
                .reqId(ex.getReqId())
                .traceId(traceId)
                .build();

        logError(ex, traceId);

        return new ResponseEntity<ErrorResponse>(response, ex.getErrorCode().getHttpStatus());
    }

    // ─── TaxServiceException (catch-all for domain exceptions) ────────

    /**
     * 기본 도메인 예외 처리 (위의 하위 클래스에 매칭되지 않은 경우)
     */
    @ExceptionHandler(TaxServiceException.class)
    public ResponseEntity<ErrorResponse> handleTaxServiceException(
            TaxServiceException ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getDetailMessage())
                .reqId(ex.getReqId())
                .traceId(traceId)
                .build();

        logError(ex, traceId);

        return new ResponseEntity<ErrorResponse>(response, ex.getErrorCode().getHttpStatus());
    }

    // ─── Spring Validation (MethodArgumentNotValidException) ──────────

    /**
     * Spring Bean Validation (@Valid) 실패 시 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (org.springframework.validation.FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("field", fe.getField());
            detail.put("issue", fe.getDefaultMessage());
            detail.put("received", fe.getRejectedValue() != null
                    ? fe.getRejectedValue().toString() : null);
            details.add(detail);
        }

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED)
                .message(ErrorCode.VALIDATION_FAILED.getDefaultMessage())
                .details(details)
                .traceId(traceId)
                .build();

        LOG_CALCULATION.warn("[traceId={}] Bean Validation 실패: field errors={}",
                traceId, details.size());

        return new ResponseEntity<ErrorResponse>(response, HttpStatus.BAD_REQUEST);
    }

    // ─── Generic Exception (fallback) ─────────────────────────────────

    /**
     * 예상하지 못한 모든 예외에 대한 최종 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        String traceId = generateTraceId();

        log.error("[traceId={}] 처리되지 않은 예외 발생", traceId, ex);
        LOG_CALCULATION.error("[traceId={}] 시스템 오류 발생: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_ERROR)
                .message(ErrorCode.INTERNAL_ERROR.getDefaultMessage())
                .traceId(traceId)
                .build();

        return new ResponseEntity<ErrorResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ─── Private helpers ──────────────────────────────────────────────

    /**
     * 에러를 LOG_CALCULATION 로거와 기본 로거에 기록한다.
     */
    private void logError(TaxServiceException ex, String traceId) {
        String errorCode = ex.getErrorCode().getCode();
        String reqId = ex.getReqId();

        log.error("[traceId={}, reqId={}, errorCode={}] {}",
                traceId, reqId, errorCode, ex.getDetailMessage(), ex);

        LOG_CALCULATION.error("[traceId={}, reqId={}, errorCode={}] {}",
                traceId, reqId, errorCode, ex.getDetailMessage());
    }

    /**
     * 요청별 고유 추적 ID 를 생성한다.
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
