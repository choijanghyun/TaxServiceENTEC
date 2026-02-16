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
 * REF_TAX_RATE 테이블 엔티티.
 * <p>
 * 법인세 기본 세율표를 관리한다.
 * 과세표준 구간별 세율 및 누진공제액 정보를 포함한다.
 * </p>
 */
@Entity
@Table(name = "REF_TAX_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefTaxRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;

    /** 과세표준 하한 */
    @Column(name = "bracket_min")
    private Long bracketMin;

    /** 과세표준 상한 */
    @Column(name = "bracket_max")
    private Long bracketMax;

    /** 세율 (%) */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** 누진공제액 */
    @Column(name = "progressive_deduction")
    private Long progressiveDeduction;
}
