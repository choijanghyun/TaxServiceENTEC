package com.entec.tax.common.exception;

/**
 * 에러 코드 정의
 * - 시스템 전체에서 사용되는 에러 코드를 Enum으로 관리
 * - HTTP 상태 코드, 에러 코드, 에러 메시지를 포함
 *
 * 코드 체계:
 *   COM_XXX: 공통 에러
 *   AUTH_XXX: 인증/인가 에러
 *   TAX_XXX: 법인세 업무 에러
 *   FILE_XXX: 파일 관련 에러
 *   DB_XXX: 데이터베이스 에러
 */
public enum ErrorCode {

    // === 공통 에러 (COM) ===
    SUCCESS(200, "COM_000", "정상 처리되었습니다."),
    BAD_REQUEST(400, "COM_001", "잘못된 요청입니다."),
    INVALID_PARAMETER(400, "COM_002", "유효하지 않은 파라미터입니다."),
    NOT_FOUND(404, "COM_003", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "COM_004", "허용되지 않은 HTTP 메소드입니다."),
    INTERNAL_SERVER_ERROR(500, "COM_005", "서버 내부 오류가 발생하였습니다."),
    DATA_INTEGRITY_ERROR(500, "COM_006", "데이터 무결성 오류가 발생하였습니다."),
    DUPLICATE_DATA(409, "COM_007", "중복된 데이터가 존재합니다."),

    // === 인증/인가 에러 (AUTH) ===
    UNAUTHORIZED(401, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(403, "AUTH_002", "접근 권한이 없습니다."),
    TOKEN_EXPIRED(401, "AUTH_003", "인증 토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "AUTH_004", "유효하지 않은 인증 토큰입니다."),
    SESSION_EXPIRED(401, "AUTH_005", "세션이 만료되었습니다. 다시 로그인해 주세요."),

    // === 법인세 업무 에러 (TAX) ===
    TAX_CORP_NOT_FOUND(404, "TAX_001", "법인 정보를 찾을 수 없습니다."),
    TAX_PERIOD_INVALID(400, "TAX_002", "사업연도가 유효하지 않습니다."),
    TAX_ALREADY_FILED(409, "TAX_003", "이미 신고된 건이 존재합니다."),
    TAX_AMENDMENT_NOT_ALLOWED(400, "TAX_004", "경정청구가 허용되지 않는 상태입니다."),
    TAX_CALCULATION_ERROR(500, "TAX_005", "세액 계산 중 오류가 발생하였습니다."),
    TAX_DEADLINE_EXCEEDED(400, "TAX_006", "경정청구 기한이 초과되었습니다."),
    TAX_BIZ_NO_INVALID(400, "TAX_007", "사업자등록번호가 유효하지 않습니다."),
    TAX_CORP_NO_INVALID(400, "TAX_008", "법인등록번호가 유효하지 않습니다."),
    TAX_AMOUNT_NEGATIVE(400, "TAX_009", "세액은 음수가 될 수 없습니다."),
    TAX_DATA_NOT_COMPLETE(400, "TAX_010", "필수 신고 데이터가 누락되었습니다."),

    // === 파일 관련 에러 (FILE) ===
    FILE_NOT_FOUND(404, "FILE_001", "파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAIL(500, "FILE_002", "파일 업로드에 실패하였습니다."),
    FILE_SIZE_EXCEEDED(400, "FILE_003", "파일 크기가 제한을 초과하였습니다."),
    FILE_TYPE_NOT_ALLOWED(400, "FILE_004", "허용되지 않은 파일 형식입니다."),
    FILE_READ_ERROR(500, "FILE_005", "파일 읽기 중 오류가 발생하였습니다."),

    // === 데이터베이스 에러 (DB) ===
    DB_CONNECTION_ERROR(500, "DB_001", "데이터베이스 연결에 실패하였습니다."),
    DB_QUERY_ERROR(500, "DB_002", "데이터 조회 중 오류가 발생하였습니다."),
    DB_INSERT_ERROR(500, "DB_003", "데이터 등록 중 오류가 발생하였습니다."),
    DB_UPDATE_ERROR(500, "DB_004", "데이터 수정 중 오류가 발생하였습니다."),
    DB_DELETE_ERROR(500, "DB_005", "데이터 삭제 중 오류가 발생하였습니다."),
    DB_TRANSACTION_ERROR(500, "DB_006", "트랜잭션 처리 중 오류가 발생하였습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
