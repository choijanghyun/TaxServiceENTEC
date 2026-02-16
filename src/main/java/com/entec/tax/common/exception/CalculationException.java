package com.entec.tax.common.exception;

/**
 * 세액 계산 단계에서 발생하는 예외
 * <p>
 * 실패한 계산 단계(M3, M4, M5, M6 등)를 {@code calcStep}으로 명시한다.
 */
public class CalculationException extends TaxServiceException {

    private static final long serialVersionUID = 1L;

    private final String calcStep;

    /**
     * @param errorCode 에러 코드
     * @param message   상세 메시지
     * @param reqId     요청 추적 ID
     * @param calcStep  실패한 계산 단계 코드 (예: "M3", "M4", "M5", "M6")
     */
    public CalculationException(ErrorCode errorCode, String message, String reqId, String calcStep) {
        super(errorCode, message, reqId);
        this.calcStep = calcStep;
    }

    /**
     * @param errorCode 에러 코드
     * @param message   상세 메시지
     * @param reqId     요청 추적 ID
     * @param calcStep  실패한 계산 단계 코드
     * @param cause     원인 예외
     */
    public CalculationException(ErrorCode errorCode, String message, String reqId,
                                String calcStep, Throwable cause) {
        super(errorCode, message, reqId, cause);
        this.calcStep = calcStep;
    }

    /**
     * @param message  상세 메시지
     * @param reqId    요청 추적 ID
     * @param calcStep 실패한 계산 단계 코드
     */
    public CalculationException(String message, String reqId, String calcStep) {
        this(ErrorCode.CALCULATION_STEP_FAILED, message, reqId, calcStep);
    }

    public String getCalcStep() {
        return calcStep;
    }
}
