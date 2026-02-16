package com.entec.tax.common.exception;

/**
 * 요청한 리소스를 찾을 수 없는 경우의 예외
 * <p>
 * 존재하지 않는 요청 ID 조회 등에서 사용한다.
 */
public class RequestNotFoundException extends TaxServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 상세 메시지
     * @param reqId   요청 추적 ID
     */
    public RequestNotFoundException(String message, String reqId) {
        super(ErrorCode.REQUEST_NOT_FOUND, message, reqId);
    }

    /**
     * @param errorCode 에러 코드
     * @param message   상세 메시지
     * @param reqId     요청 추적 ID
     */
    public RequestNotFoundException(ErrorCode errorCode, String message, String reqId) {
        super(errorCode, message, reqId);
    }
}
