package com.entec.tax.domain.output.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OUT_REFUND 테이블 엔티티 (§22).
 * <p>
 * 환급 산출 결과를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_REFUND")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutRefund {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 기존 산출세액 */
    @Column(name = "existing_computed_tax")
    private Long existingComputedTax;

    /** 기존 공제액 */
    @Column(name = "existing_deductions")
    private Long existingDeductions;

    /** 기존 결정세액 */
    @Column(name = "existing_determined_tax")
    private Long existingDeterminedTax;

    /** 기존 납부세액 */
    @Column(name = "existing_paid_tax")
    private Long existingPaidTax;

    /** 신규 산출세액 */
    @Column(name = "new_computed_tax")
    private Long newComputedTax;

    /** 신규 공제액 */
    @Column(name = "new_deductions")
    private Long newDeductions;

    /** 신규 최저한세 조정액 */
    @Column(name = "new_min_tax_adj")
    private Long newMinTaxAdj;

    /** 신규 결정세액 */
    @Column(name = "new_determined_tax")
    private Long newDeterminedTax;

    /** 농어촌특별세 합계 */
    @Column(name = "nongteuk_total")
    private Long nongteukTotal;

    /** 환급 금액 */
    @Column(name = "refund_amount")
    private Long refundAmount;

    /** 환급 이자 기산일 */
    @Column(name = "refund_interest_start")
    private LocalDate refundInterestStart;

    /** 환급 이자 종료일 */
    @Column(name = "refund_interest_end", length = 20)
    private String refundInterestEnd;

    /** 환급 이자율 */
    @Column(name = "refund_interest_rate", precision = 10, scale = 6)
    private BigDecimal refundInterestRate;

    /** 환급 이자 금액 */
    @Column(name = "refund_interest_amount")
    private Long refundInterestAmount;

    /** 중간 환급 금액 */
    @Column(name = "interim_refund_amount")
    private Long interimRefundAmount;

    /** 중간 이자 금액 */
    @Column(name = "interim_interest_amount")
    private Long interimInterestAmount;

    /** 지방세 환급 */
    @Column(name = "local_tax_refund")
    private Long localTaxRefund;

    /** 총 기대 금액 */
    @Column(name = "total_expected")
    private Long totalExpected;

    /** 환급 한도 상세 */
    @Column(name = "refund_cap_detail", columnDefinition = "TEXT")
    private String refundCapDetail;

    /** 최적 조합 ID */
    @Column(name = "optimal_combo_id", length = 30)
    private String optimalComboId;

    /** 이월공제 금액 */
    @Column(name = "carryforward_credits")
    private Long carryforwardCredits;

    /** 이월공제 상세 */
    @Column(name = "carryforward_detail", columnDefinition = "TEXT")
    private String carryforwardDetail;

    /** 가산세 변동 */
    @Column(name = "penalty_tax_change")
    private Long penaltyTaxChange;

    @Builder
    public OutRefund(String reqId, Long existingComputedTax, Long existingDeductions,
                     Long existingDeterminedTax, Long existingPaidTax,
                     Long newComputedTax, Long newDeductions, Long newMinTaxAdj,
                     Long newDeterminedTax, Long nongteukTotal, Long refundAmount,
                     LocalDate refundInterestStart, String refundInterestEnd,
                     BigDecimal refundInterestRate, Long refundInterestAmount,
                     Long interimRefundAmount, Long interimInterestAmount,
                     Long localTaxRefund, Long totalExpected, String refundCapDetail,
                     String optimalComboId, Long carryforwardCredits,
                     String carryforwardDetail, Long penaltyTaxChange) {
        this.reqId = reqId;
        this.existingComputedTax = existingComputedTax;
        this.existingDeductions = existingDeductions;
        this.existingDeterminedTax = existingDeterminedTax;
        this.existingPaidTax = existingPaidTax;
        this.newComputedTax = newComputedTax;
        this.newDeductions = newDeductions;
        this.newMinTaxAdj = newMinTaxAdj;
        this.newDeterminedTax = newDeterminedTax;
        this.nongteukTotal = nongteukTotal;
        this.refundAmount = refundAmount;
        this.refundInterestStart = refundInterestStart;
        this.refundInterestEnd = refundInterestEnd;
        this.refundInterestRate = refundInterestRate;
        this.refundInterestAmount = refundInterestAmount;
        this.interimRefundAmount = interimRefundAmount;
        this.interimInterestAmount = interimInterestAmount;
        this.localTaxRefund = localTaxRefund;
        this.totalExpected = totalExpected;
        this.refundCapDetail = refundCapDetail;
        this.optimalComboId = optimalComboId;
        this.carryforwardCredits = carryforwardCredits;
        this.carryforwardDetail = carryforwardDetail;
        this.penaltyTaxChange = penaltyTaxChange;
    }
}
