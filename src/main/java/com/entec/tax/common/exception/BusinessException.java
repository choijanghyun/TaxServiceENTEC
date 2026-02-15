package com.entec.tax.common.exception;

/**
 * 비즈니스 예외 클래스
 * - 업무 로직에서 발생하는 예외를 처리하기 위한 커스텀 예외
 * - ErrorCode를 활용하여 일관된 에러 응답 제공
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String detailMessage;

    /**
     * ErrorCode만으로 예외 생성
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    /**
     * ErrorCode + 상세 메시지로 예외 생성
     */
    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    /**
     * ErrorCode + 원인 예외로 예외 생성
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.detailMessage = null;
    }

    /**
     * ErrorCode + 상세 메시지 + 원인 예외로 예외 생성
     */
    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
