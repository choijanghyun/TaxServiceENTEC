package com.entec.tax.domain.check.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * CHK_INSPECTION_LOG 테이블 엔티티 (§16).
 * <p>
 * 점검 항목별 검사 로그를 관리한다.
 * </p>
 */
@Entity
@Table(name = "CHK_INSPECTION_LOG")
@IdClass(ChkInspectionLogId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChkInspectionLog {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 점검 코드 (PK) */
    @Id
    @Column(name = "inspection_code", length = 30, nullable = false)
    private String inspectionCode;

    /** 점검 항목명 */
    @Column(name = "inspection_name", length = 100)
    private String inspectionName;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    /** 판정 결과 */
    @Column(name = "judgment", length = 30)
    private String judgment;

    /** 요약 */
    @Column(name = "summary", length = 500)
    private String summary;

    /** 관련 모듈 */
    @Column(name = "related_module", length = 50)
    private String relatedModule;

    /** 산출 금액 */
    @Column(name = "calculated_amount")
    private Long calculatedAmount;

    /** 정렬 순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 검사 일시 */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    public ChkInspectionLog(String reqId, String inspectionCode,
                             String inspectionName, String legalBasis,
                             String judgment, String summary,
                             String relatedModule, Long calculatedAmount,
                             Integer sortOrder, LocalDateTime checkedAt) {
        this.reqId = reqId;
        this.inspectionCode = inspectionCode;
        this.inspectionName = inspectionName;
        this.legalBasis = legalBasis;
        this.judgment = judgment;
        this.summary = summary;
        this.relatedModule = relatedModule;
        this.calculatedAmount = calculatedAmount;
        this.sortOrder = sortOrder;
        this.checkedAt = checkedAt;
    }
}
