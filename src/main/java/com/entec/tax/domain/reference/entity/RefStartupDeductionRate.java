package com.entec.tax.domain.reference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * REF_STARTUP_DEDUCTION_RATE 테이블 엔티티.
 * <p>
 * 창업 중소기업 세액감면율 정보를 관리한다.
 * 창업자 유형·소재지별 감면율 및 적용 기간을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_STARTUP_DEDUCTION_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefStartupDeductionRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** 창업자 유형 (청년/일반 등) */
    @Column(name = "founder_type", length = 20)
    private String founderType;

    /** 소재지 유형 (수도권과밀억제/비수도권/인구감소 등) */
    @Column(name = "location_type", length = 30)
    private String locationType;

    /** 감면율 (%) */
    @Column(name = "deduction_rate", precision = 5, scale = 2)
    private BigDecimal deductionRate;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;

    /** 비고 */
    @Column(name = "remark", length = 200)
    private String remark;
}
