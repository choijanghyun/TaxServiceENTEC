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
 * REF_DEEMED_INTEREST_RATE 테이블 엔티티.
 * <p>
 * 인정이자율 정보를 관리한다.
 * 연도·이자율 유형별 인정이자율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_DEEMED_INTEREST_RATE")
@IdClass(RefDeemedInterestRateId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDeemedInterestRate {

    /** 연도 (PK) */
    @Id
    @Column(name = "year", length = 4, nullable = false)
    private String year;

    /** 이자율 유형 (PK) */
    @Id
    @Column(name = "rate_type", length = 20, nullable = false)
    private String rateType;

    /** 인정이자율 (%) */
    @Column(name = "rate", precision = 5, scale = 2)
    private BigDecimal rate;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;
}
