package com.entec.tax.common.exception;

/**
 * 동일 요청에 대해 동시 처리 충돌이 발생한 경우의 예외
 * <p>
 * 낙관적 잠금(Optimistic Lock) 또는 중복 요청 감지 시 사용한다.
 */
public class ConcurrentConflictException extends TaxServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message 상세 메시지
     * @param reqId   요청 추적 ID
     */
    public ConcurrentConflictException(String message, String reqId) {
        super(ErrorCode.CONCURRENT_CONFLICT, message, reqId);
    }

    /**
     * @param errorCode 에러 코드
     * @param message   상세 메시지
     * @param reqId     요청 추적 ID
     */
    public ConcurrentConflictException(ErrorCode errorCode, String message, String reqId) {
        super(errorCode, message, reqId);
    }

    /**
     * @param message 상세 메시지
     * @param reqId   요청 추적 ID
     * @param cause   원인 예외
     */
    public ConcurrentConflictException(String message, String reqId, Throwable cause) {
        super(ErrorCode.CONCURRENT_CONFLICT, message, reqId, cause);
    }
}
