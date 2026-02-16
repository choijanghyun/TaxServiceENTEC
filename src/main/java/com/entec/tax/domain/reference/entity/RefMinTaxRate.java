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
 * REF_MIN_TAX_RATE 테이블 엔티티.
 * <p>
 * 최저한세율 정보를 관리한다.
 * 기업 규모별·과세표준 구간별 최저한세율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_MIN_TAX_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefMinTaxRate {

    /** 최저한세 ID (PK) */
    @Id
    @Column(name = "min_tax_id", nullable = false)
    private Integer minTaxId;

    /** 기업 규모 (대/중견/중소 등) */
    @Column(name = "corp_size", length = 10)
    private String corpSize;

    /** 과세표준 하한 */
    @Column(name = "bracket_min")
    private Long bracketMin;

    /** 과세표준 상한 */
    @Column(name = "bracket_max")
    private Long bracketMax;

    /** 최저한세율 (%) */
    @Column(name = "min_rate", precision = 5, scale = 2)
    private BigDecimal minRate;
}
