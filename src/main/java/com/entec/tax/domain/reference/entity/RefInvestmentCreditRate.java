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
 * REF_INVESTMENT_CREDIT_RATE 테이블 엔티티.
 * <p>
 * 투자 세액공제율 정보를 관리한다.
 * 투자 유형·기업 규모별 기본공제율 및 추가공제율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INVESTMENT_CREDIT_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefInvestmentCreditRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** 적용 시작 연도 */
    @Column(name = "tax_year_from", length = 4)
    private String taxYearFrom;

    /** 투자 유형 */
    @Column(name = "invest_type", length = 30)
    private String investType;

    /** 기업 규모 */
    @Column(name = "corp_size", length = 10)
    private String corpSize;

    /** 기본 공제율 (%) */
    @Column(name = "basic_rate", precision = 5, scale = 2)
    private BigDecimal basicRate;

    /** 추가 공제율 (%) */
    @Column(name = "additional_rate", precision = 5, scale = 2)
    private BigDecimal additionalRate;
}
