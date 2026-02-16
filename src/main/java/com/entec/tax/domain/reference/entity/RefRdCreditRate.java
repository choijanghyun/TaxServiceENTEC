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
 * REF_RD_CREDIT_RATE 테이블 엔티티.
 * <p>
 * 연구·인력개발비 세액공제율 정보를 관리한다.
 * R&D 유형·산출 방식·기업 규모별 공제율 및 최저한세 특례 여부를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_RD_CREDIT_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefRdCreditRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** R&D 유형 (일반/신성장·원천) */
    @Column(name = "rd_type", length = 20)
    private String rdType;

    /** 산출 방식 (당기분/증가분) */
    @Column(name = "method", length = 10)
    private String method;

    /** 기업 규모 */
    @Column(name = "corp_size", length = 10)
    private String corpSize;

    /** 공제율 (%) */
    @Column(name = "credit_rate", precision = 5, scale = 2)
    private BigDecimal creditRate;

    /** 최저한세 특례 면제 유형 */
    @Column(name = "min_tax_exempt", length = 20)
    private String minTaxExempt;
}
