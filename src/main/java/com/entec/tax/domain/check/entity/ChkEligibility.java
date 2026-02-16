package com.entec.tax.domain.check.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CHK_ELIGIBILITY 테이블 엔티티 (§15).
 * <p>
 * 세액공제 환급 적격 여부 검증 결과를 관리한다.
 * </p>
 */
@Entity
@Table(name = "CHK_ELIGIBILITY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChkEligibility {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 세금 유형 */
    @Column(name = "tax_type", length = 10)
    private String taxType;

    /** 기업 규모 */
    @Column(name = "company_size", length = 20)
    private String companySize;

    /** 수도권 구분 */
    @Column(name = "capital_zone", length = 20)
    private String capitalZone;

    /** 신고 기한 */
    @Column(name = "filing_deadline")
    private LocalDate filingDeadline;

    /** 청구 기한 */
    @Column(name = "claim_deadline")
    private LocalDate claimDeadline;

    /** 기한 적격 여부 */
    @Column(name = "deadline_eligible", length = 20)
    private String deadlineEligible;

    /** 중소기업 적격 여부 */
    @Column(name = "sme_eligible", length = 20)
    private String smeEligible;

    /** 중소기업 유예 종료 연도 */
    @Column(name = "sme_grace_end_year", length = 4)
    private String smeGraceEndYear;

    /** 소기업/중기업 구분 */
    @Column(name = "small_vs_medium", length = 20)
    private String smallVsMedium;

    /** 벤처기업 확인 여부 */
    @Column(name = "venture_confirmed")
    private Boolean ventureConfirmed;

    /** 결산 확인 결과 */
    @Column(name = "settlement_check_result", length = 50)
    private String settlementCheckResult;

    /** 결산 차단 항목 */
    @Column(name = "settlement_blocked_items", columnDefinition = "TEXT")
    private String settlementBlockedItems;

    /** 추계 검사 여부 */
    @Column(name = "estimate_check")
    private Boolean estimateCheck;

    /** 성실신고 대상 여부 */
    @Column(name = "sincerity_target")
    private Boolean sincerityTarget;

    /** 종합 상태 */
    @Column(name = "overall_status", length = 30)
    private String overallStatus;

    /** 진단 상세 */
    @Column(name = "diagnosis_detail", columnDefinition = "TEXT")
    private String diagnosisDetail;

    /** 검사 일시 */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    public ChkEligibility(String reqId, String taxType, String companySize,
                           String capitalZone, LocalDate filingDeadline,
                           LocalDate claimDeadline, String deadlineEligible,
                           String smeEligible, String smeGraceEndYear,
                           String smallVsMedium, Boolean ventureConfirmed,
                           String settlementCheckResult, String settlementBlockedItems,
                           Boolean estimateCheck, Boolean sincerityTarget,
                           String overallStatus, String diagnosisDetail,
                           LocalDateTime checkedAt) {
        this.reqId = reqId;
        this.taxType = taxType;
        this.companySize = companySize;
        this.capitalZone = capitalZone;
        this.filingDeadline = filingDeadline;
        this.claimDeadline = claimDeadline;
        this.deadlineEligible = deadlineEligible;
        this.smeEligible = smeEligible;
        this.smeGraceEndYear = smeGraceEndYear;
        this.smallVsMedium = smallVsMedium;
        this.ventureConfirmed = ventureConfirmed;
        this.settlementCheckResult = settlementCheckResult;
        this.settlementBlockedItems = settlementBlockedItems;
        this.estimateCheck = estimateCheck;
        this.sincerityTarget = sincerityTarget;
        this.overallStatus = overallStatus;
        this.diagnosisDetail = diagnosisDetail;
        this.checkedAt = checkedAt;
    }
}
