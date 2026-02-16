package com.entec.tax.common.constants;

/**
 * 요청 처리 상태 코드
 */
public enum RequestStatus {

    RECEIVED("RECEIVED", "접수완료"),
    PARSING("PARSING", "파싱중"),
    PARSED("PARSED", "파싱완료"),
    CHECKING("CHECKING", "요건검토중"),
    CALCULATING("CALCULATING", "세액계산중"),
    OPTIMIZING("OPTIMIZING", "최적화중"),
    REPORTING("REPORTING", "리포트생성중"),
    COMPLETED("COMPLETED", "처리완료"),
    ERROR("ERROR", "오류발생");

    private final String code;
    private final String description;

    RequestStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드 문자열로부터 RequestStatus 를 반환한다.
     *
     * @param code 코드 문자열 (예: "RECEIVED", "PARSING")
     * @return 매칭되는 RequestStatus
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static RequestStatus fromCode(String code) {
        for (RequestStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RequestStatus code: " + code);
    }
}
