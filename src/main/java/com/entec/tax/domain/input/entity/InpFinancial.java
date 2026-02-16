package com.entec.tax.domain.input.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * INP_FINANCIAL 테이블 엔티티.
 * <p>
 * 요청 건의 재무/세무 신고 관련 상세 정보를 저장한다.
 * 설계문서 section 13 기반.
 * </p>
 */
@Entity
@Table(name = "INP_FINANCIAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InpFinancial {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 사업소득금액 (원) */
    @Column(name = "biz_income")
    private Long bizIncome;

    /** 비과세소득 (원) */
    @Column(name = "non_taxable_income")
    private Long nonTaxableIncome;

    /** 이월결손금 합계 (원) */
    @Column(name = "loss_carryforward_total")
    private Long lossCarryforwardTotal;

    /** 이월결손금 상세 내역 (JSON) */
    @Lob
    @Column(name = "loss_carryforward_detail")
    private String lossCarryforwardDetail;

    /** 중간예납세액 (원) */
    @Column(name = "interim_prepaid_tax")
    private Long interimPrepaidTax;

    /** 원천징수세액 (원) */
    @Column(name = "withholding_tax")
    private Long withholdingTax;

    /** 결정세액 (원) */
    @Column(name = "determined_tax")
    private Long determinedTax;

    /** 배당소득 합계 (원) */
    @Column(name = "dividend_income_total")
    private Long dividendIncomeTotal;

    /** 배당소득 제외 상세 내역 (JSON) */
    @Lob
    @Column(name = "dividend_exclusion_detail")
    private String dividendExclusionDetail;

    /** 외국납부세액 합계 (원) */
    @Column(name = "foreign_tax_total")
    private Long foreignTaxTotal;

    /** 국외원천소득 합계 (원) */
    @Column(name = "foreign_income_total")
    private Long foreignIncomeTotal;

    /** 세무조정 상세 내역 (JSON) */
    @Lob
    @Column(name = "tax_adjustment_detail")
    private String taxAdjustmentDetail;

    /** 종합소득공제 합계 (원) */
    @Column(name = "inc_deduction_total")
    private Long incDeductionTotal;

    /** 종합소득공제 상세 내역 (JSON) */
    @Lob
    @Column(name = "inc_deduction_detail")
    private String incDeductionDetail;

    /** 종합소득금액 (원) */
    @Column(name = "inc_comprehensive_income")
    private Long incComprehensiveIncome;

    /** 당기 결손금 (원) */
    @Column(name = "current_year_loss")
    private Long currentYearLoss;

    /** 전기 납부세액 (원) */
    @Column(name = "prior_year_tax_paid")
    private Long priorYearTaxPaid;

    /** 수정신고 이력 (JSON) */
    @Lob
    @Column(name = "amendment_history")
    private String amendmentHistory;

    /** 업무용 차량 관련 비용 상세 (JSON) */
    @Lob
    @Column(name = "vehicle_expense_detail")
    private String vehicleExpenseDetail;

    @Builder
    public InpFinancial(String reqId, Long bizIncome, Long nonTaxableIncome,
                        Long lossCarryforwardTotal, String lossCarryforwardDetail,
                        Long interimPrepaidTax, Long withholdingTax, Long determinedTax,
                        Long dividendIncomeTotal, String dividendExclusionDetail,
                        Long foreignTaxTotal, Long foreignIncomeTotal,
                        String taxAdjustmentDetail,
                        Long incDeductionTotal, String incDeductionDetail,
                        Long incComprehensiveIncome, Long currentYearLoss,
                        Long priorYearTaxPaid, String amendmentHistory,
                        String vehicleExpenseDetail) {
        this.reqId = reqId;
        this.bizIncome = bizIncome;
        this.nonTaxableIncome = nonTaxableIncome;
        this.lossCarryforwardTotal = lossCarryforwardTotal;
        this.lossCarryforwardDetail = lossCarryforwardDetail;
        this.interimPrepaidTax = interimPrepaidTax;
        this.withholdingTax = withholdingTax;
        this.determinedTax = determinedTax;
        this.dividendIncomeTotal = dividendIncomeTotal;
        this.dividendExclusionDetail = dividendExclusionDetail;
        this.foreignTaxTotal = foreignTaxTotal;
        this.foreignIncomeTotal = foreignIncomeTotal;
        this.taxAdjustmentDetail = taxAdjustmentDetail;
        this.incDeductionTotal = incDeductionTotal;
        this.incDeductionDetail = incDeductionDetail;
        this.incComprehensiveIncome = incComprehensiveIncome;
        this.currentYearLoss = currentYearLoss;
        this.priorYearTaxPaid = priorYearTaxPaid;
        this.amendmentHistory = amendmentHistory;
        this.vehicleExpenseDetail = vehicleExpenseDetail;
    }
}
