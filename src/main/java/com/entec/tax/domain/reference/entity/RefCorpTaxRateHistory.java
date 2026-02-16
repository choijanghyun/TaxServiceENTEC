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
 * REF_CORP_TAX_RATE_HISTORY 테이블 엔티티.
 * <p>
 * 법인세 세율 변경 이력을 관리한다.
 * 연도별 과세표준 구간·세율·누진공제액의 변경 내역을 보관한다.
 * </p>
 */
@Entity
@Table(name = "REF_CORP_TAX_RATE_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefCorpTaxRateHistory {

    /** 세율 이력 ID (PK) */
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
