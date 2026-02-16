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
 * REF_INC_MIN_TAX 테이블 엔티티.
 * <p>
 * 개인(소득세) 최저한세 기준 정보를 관리한다.
 * 적용 시작 연도별 기준 금액 및 초과·미만 세율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INC_MIN_TAX")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefIncMinTax {

    /** 적용 시작 연도 (PK) */
    @Id
    @Column(name = "effective_from", length = 4, nullable = false)
    private String effectiveFrom;

    /** 기준 금액 */
    @Column(name = "threshold")
    private Long threshold;

    /** 기준 금액 이하 세율 (%) */
    @Column(name = "rate_below", precision = 5, scale = 2)
    private BigDecimal rateBelow;

    /** 기준 금액 초과 세율 (%) */
    @Column(name = "rate_above", precision = 5, scale = 2)
    private BigDecimal rateAbove;
}
