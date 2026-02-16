package com.entec.tax.domain.reference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * REF_INC_TAX_RATE 테이블 엔티티.
 * <p>
 * 개인(소득세) 기본 세율표를 관리한다.
 * 적용 시작 연도·구간 번호별 과세표준 구간·세율·누진공제액을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INC_TAX_RATE")
@IdClass(RefIncTaxRateId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefIncTaxRate {

    /** 적용 시작 연도 (PK) */
    @Id
    @Column(name = "effective_from", length = 4, nullable = false)
    private String effectiveFrom;

    /** 구간 번호 (PK) */
    @Id
    @Column(name = "bracket_no", nullable = false)
    private Integer bracketNo;

    /** 과세표준 하한 */
    @Column(name = "lower_limit")
    private Long lowerLimit;

    /** 과세표준 상한 */
    @Column(name = "upper_limit")
    private Long upperLimit;

    /** 세율 (%) */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** 누진공제액 */
    @Column(name = "progressive_deduction")
    private Long progressiveDeduction;
}
