package com.entec.tax.common.exception;

/**
 * 세금 환급 서비스 기본 예외 클래스
 * <p>
 * 모든 도메인 예외는 이 클래스를 상속하며,
 * {@link ErrorCode}, 상세 메시지, 요청 ID 를 공통으로 포함한다.
 */
public class TaxServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String detailMessage;
    private final String reqId;

    /**
     * @param errorCode     에러 코드
     * @param detailMessage 사용자/개발자 확인용 상세 메시지
     * @param reqId         요청 추적 ID
     */
    public TaxServiceException(ErrorCode errorCode, String detailMessage, String reqId) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.reqId = reqId;
    }

    /**
     * @param errorCode     에러 코드
     * @param detailMessage 사용자/개발자 확인용 상세 메시지
     * @param reqId         요청 추적 ID
     * @param cause         원인 예외
     */
    public TaxServiceException(ErrorCode errorCode, String detailMessage, String reqId, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.reqId = reqId;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getReqId() {
        return reqId;
    }
}
