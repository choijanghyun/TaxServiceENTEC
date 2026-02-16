package com.entec.tax.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 전역 에러 코드 정의
 * <p>
 * 코드 접두사 규칙:
 * <ul>
 *   <li>ERR_VAL  - 입력 검증 오류</li>
 *   <li>ERR_CALC - 세액 계산 오류</li>
 *   <li>ERR_HARD - 결산조정 차단(Hard-Fail) 오류</li>
 *   <li>ERR_CONC - 동시성 충돌 오류</li>
 *   <li>ERR_NOTF - 리소스 미발견 오류</li>
 *   <li>ERR_SYS  - 시스템/내부 오류</li>
 * </ul>
 */
public enum ErrorCode {

    // ── 검증(Validation) ────────────────────────────────────────────────
    VALIDATION_FAILED("ERR_VAL_001", "입력 값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    INVALID_TAX_TYPE("ERR_VAL_002", "지원하지 않는 세금 유형입니다.", HttpStatus.BAD_REQUEST),
    INVALID_FISCAL_YEAR("ERR_VAL_003", "사업연도 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("ERR_VAL_004", "필수 입력 항목이 누락되었습니다.", HttpStatus.BAD_REQUEST),

    // ── 계산(Calculation) ───────────────────────────────────────────────
    CALCULATION_FAILED("ERR_CALC_001", "세액 계산 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CALCULATION_STEP_FAILED("ERR_CALC_002", "특정 계산 단계에서 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    CALCULATION_OVERFLOW("ERR_CALC_003", "계산 결과가 허용 범위를 초과했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ── 결산조정 차단(Hard-Fail) ────────────────────────────────────────
    HARD_FAIL("ERR_HARD_001", "결산조정 차단 항목이 존재합니다.", HttpStatus.UNPROCESSABLE_ENTITY),
    HARD_FAIL_BLOCKED("ERR_HARD_002", "차단 항목으로 인해 처리를 계속할 수 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY),

    // ── 동시성(Concurrency) ─────────────────────────────────────────────
    CONCURRENT_CONFLICT("ERR_CONC_001", "동일 요청에 대해 동시 처리 충돌이 발생했습니다.", HttpStatus.CONFLICT),
    INVALID_STATUS("ERR_CONC_002", "현재 상태에서 불가능한 작업입니다.", HttpStatus.CONFLICT),

    // ── 리소스 미발견(Not Found) ────────────────────────────────────────
    REQUEST_NOT_FOUND("ERR_NOTF_001", "요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("ERR_NOTF_002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // ── 시스템(System) ──────────────────────────────────────────────────
    INTERNAL_ERROR("ERR_SYS_001", "내부 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("ERR_SYS_002", "서비스를 일시적으로 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT("ERR_SYS_003", "요청 처리 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),

    // ── JSON/입력(Input) ──────────────────────────────────────────────
    INVALID_JSON("ERR_VAL_005", "유효하지 않은 JSON 형식입니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_CATEGORY("ERR_VAL_006", "중복된 카테고리가 존재합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * 코드 문자열로부터 ErrorCode 를 반환한다.
     *
     * @param code 코드 문자열 (예: "ERR_VAL_001")
     * @return 매칭되는 ErrorCode
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        throw new IllegalArgumentException("Unknown ErrorCode: " + code);
    }
}
