package com.entec.tax.domain.output.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * OUT_CREDIT_DETAIL 테이블 엔티티 (§19).
 * <p>
 * 개별 세액공제 항목별 산출 상세를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_CREDIT_DETAIL")
@IdClass(OutCreditDetailId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutCreditDetail {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 항목 ID (PK) */
    @Id
    @Column(name = "item_id", length = 30, nullable = false)
    private String itemId;

    /** 항목명 */
    @Column(name = "item_name", length = 100)
    private String itemName;

    /** 조항 */
    @Column(name = "provision", length = 50)
    private String provision;

    /** 공제 유형 */
    @Column(name = "credit_type", length = 30)
    private String creditType;

    /** 항목 상태 */
    @Column(name = "item_status", length = 20)
    private String itemStatus;

    /** 총 공제 금액 */
    @Column(name = "gross_amount")
    private Long grossAmount;

    /** 농어촌특별세 면제 여부 */
    @Column(name = "nongteuk_exempt")
    private Boolean nongteukExempt;

    /** 농어촌특별세 금액 */
    @Column(name = "nongteuk_amount")
    private Long nongteukAmount;

    /** 순 공제 금액 */
    @Column(name = "net_amount")
    private Long netAmount;

    /** 최저한세 대상 여부 */
    @Column(name = "min_tax_subject")
    private Boolean minTaxSubject;

    /** 이월공제 여부 */
    @Column(name = "is_carryforward")
    private Boolean isCarryforward;

    /** 이월공제 금액 */
    @Column(name = "carryforward_amount")
    private Long carryforwardAmount;

    /** 일몰 기한 */
    @Column(name = "sunset_date", length = 20)
    private String sunsetDate;

    /** 공제율 */
    @Column(name = "deduction_rate", length = 30)
    private String deductionRate;

    /** 적용 조건 */
    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    /** 필수 서류 */
    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    /** 배제 항목 */
    @Column(name = "exclusion_items", columnDefinition = "TEXT")
    private String exclusionItems;

    /** 비고 */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** 귀속 연도 */
    @Column(name = "tax_year", length = 4)
    private String taxYear;

    /** R&D 유형 */
    @Column(name = "rd_type", length = 30)
    private String rdType;

    /** 산출 방식 */
    @Column(name = "method", length = 50)
    private String method;

    /** 산출 상세 */
    @Column(name = "calc_detail", columnDefinition = "TEXT")
    private String calcDetail;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    /** 배제 사유 */
    @Column(name = "exclusion_reasons", columnDefinition = "TEXT")
    private String exclusionReasons;

    @Builder
    public OutCreditDetail(String reqId, String itemId, String itemName,
                            String provision, String creditType, String itemStatus,
                            Long grossAmount, Boolean nongteukExempt, Long nongteukAmount,
                            Long netAmount, Boolean minTaxSubject, Boolean isCarryforward,
                            Long carryforwardAmount, String sunsetDate, String deductionRate,
                            String conditions, String requiredDocuments, String exclusionItems,
                            String notes, String taxYear, String rdType, String method,
                            String calcDetail, String legalBasis, String exclusionReasons) {
        this.reqId = reqId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.provision = provision;
        this.creditType = creditType;
        this.itemStatus = itemStatus;
        this.grossAmount = grossAmount;
        this.nongteukExempt = nongteukExempt;
        this.nongteukAmount = nongteukAmount;
        this.netAmount = netAmount;
        this.minTaxSubject = minTaxSubject;
        this.isCarryforward = isCarryforward;
        this.carryforwardAmount = carryforwardAmount;
        this.sunsetDate = sunsetDate;
        this.deductionRate = deductionRate;
        this.conditions = conditions;
        this.requiredDocuments = requiredDocuments;
        this.exclusionItems = exclusionItems;
        this.notes = notes;
        this.taxYear = taxYear;
        this.rdType = rdType;
        this.method = method;
        this.calcDetail = calcDetail;
        this.legalBasis = legalBasis;
        this.exclusionReasons = exclusionReasons;
    }
}
