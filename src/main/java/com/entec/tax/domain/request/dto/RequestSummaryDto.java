package com.entec.tax.domain.request.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * API-08 경량 요약 DTO.
 * <p>
 * 요청 건의 분석 결과를 경량 요약 형태로 반환하는 DTO이다.
 * 환급 예상 금액, 환급 이자, 지방세 환급, 최적 조합 정보 등을 포함한다.
 * </p>
 */
@Getter
@Setter
public class RequestSummaryDto {

    /** 요청 ID */
    private String reqId;

    /** 요청 처리 상태 */
    private String status;

    /** 총 예상 금액 */
    private Long totalExpected;

    /** 환급 금액 */
    private Long refundAmount;

    /** 환급 이자 금액 */
    private Long refundInterestAmount;

    /** 지방세 환급 금액 */
    private Long localTaxRefund;

    /** 최적 조합 ID */
    private Integer optimalComboId;

    /** 분석 완료 일시 */
    private String analysisCompletedAt;
}
