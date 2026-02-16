package com.entec.tax.domain.input.entity;

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
 * INP_BASIC 테이블 엔티티.
 * <p>
 * 요청 건의 기본 정보(신청자, 사업자, 세무 기본사항)를 저장한다.
 * 설계문서 section 10 기반.
 * </p>
 */
@Entity
@Table(name = "INP_BASIC")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InpBasic {

    /** 요청 ID (PK, FK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 요청 접수일 */
    @Column(name = "request_date")
    private LocalDate requestDate;

    /** 세금 유형 코드 (CORP/INC) */
    @Column(name = "tax_type", length = 4)
    private String taxType;

    /** 신청자명 (법인명/성명) */
    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    /** 사업자등록번호 */
    @Column(name = "biz_reg_no", length = 15)
    private String bizRegNo;

    /** 법인 규모 (대/중견/중소 등) */
    @Column(name = "corp_size", length = 20)
    private String corpSize;

    /** 업종 코드 */
    @Column(name = "industry_code", length = 10)
    private String industryCode;

    /** 본점 소재지 */
    @Column(name = "hq_location", length = 200)
    private String hqLocation;

    /** 수도권 여부 구분 */
    @Column(name = "capital_zone", length = 20)
    private String capitalZone;

    /** 인구감소지역 해당 여부 */
    @Column(name = "depopulation_area")
    private Boolean depopulationArea;

    /** 귀속 연도 */
    @Column(name = "tax_year", length = 4)
    private String taxYear;

    /** 사업연도 시작일 */
    @Column(name = "fiscal_start")
    private LocalDate fiscalStart;

    /** 사업연도 종료일 */
    @Column(name = "fiscal_end")
    private LocalDate fiscalEnd;

    /** 수입금액 (원) */
    @Column(name = "revenue")
    private Long revenue;

    /** 과세표준 (원) */
    @Column(name = "taxable_income")
    private Long taxableIncome;

    /** 산출세액 (원) */
    @Column(name = "computed_tax")
    private Long computedTax;

    /** 기납부세액 (원) */
    @Column(name = "paid_tax")
    private Long paidTax;

    /** 설립일 */
    @Column(name = "founding_date")
    private LocalDate foundingDate;

    /** 벤처기업 여부 */
    @Column(name = "venture_yn")
    private Boolean ventureYn;

    /** 연구개발 전담부서 보유 여부 */
    @Column(name = "rd_dept_yn")
    private Boolean rdDeptYn;

    /** 환급 청구 사유 */
    @Column(name = "claim_reason", length = 500)
    private String claimReason;

    /** 성실신고확인 대상 여부 */
    @Column(name = "sincerity_target")
    private Boolean sincerityTarget;

    /** 기장 유형 (복식부기/간편장부 등) */
    @Column(name = "bookkeeping_type", length = 20)
    private String bookkeepingType;

    /** 연결납세 여부 */
    @Column(name = "consolidated_tax")
    private Boolean consolidatedTax;

    /** 요약 정보 생성 일시 */
    @Column(name = "summary_generated_at")
    private LocalDateTime summaryGeneratedAt;

    @Builder
    public InpBasic(String reqId, LocalDate requestDate, String taxType,
                    String applicantName, String bizRegNo, String corpSize,
                    String industryCode, String hqLocation, String capitalZone,
                    Boolean depopulationArea, String taxYear,
                    LocalDate fiscalStart, LocalDate fiscalEnd,
                    Long revenue, Long taxableIncome, Long computedTax, Long paidTax,
                    LocalDate foundingDate, Boolean ventureYn, Boolean rdDeptYn,
                    String claimReason, Boolean sincerityTarget,
                    String bookkeepingType, Boolean consolidatedTax,
                    LocalDateTime summaryGeneratedAt) {
        this.reqId = reqId;
        this.requestDate = requestDate;
        this.taxType = taxType;
        this.applicantName = applicantName;
        this.bizRegNo = bizRegNo;
        this.corpSize = corpSize;
        this.industryCode = industryCode;
        this.hqLocation = hqLocation;
        this.capitalZone = capitalZone;
        this.depopulationArea = depopulationArea;
        this.taxYear = taxYear;
        this.fiscalStart = fiscalStart;
        this.fiscalEnd = fiscalEnd;
        this.revenue = revenue;
        this.taxableIncome = taxableIncome;
        this.computedTax = computedTax;
        this.paidTax = paidTax;
        this.foundingDate = foundingDate;
        this.ventureYn = ventureYn;
        this.rdDeptYn = rdDeptYn;
        this.claimReason = claimReason;
        this.sincerityTarget = sincerityTarget;
        this.bookkeepingType = bookkeepingType;
        this.consolidatedTax = consolidatedTax;
        this.summaryGeneratedAt = summaryGeneratedAt;
    }
}
