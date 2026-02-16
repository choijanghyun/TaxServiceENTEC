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
 * REF_RD_MIN_TAX_EXEMPT 테이블 엔티티.
 * <p>
 * R&D 세액공제의 최저한세 초과 적용 특례율을 관리한다.
 * R&D 유형·기업 규모별 면제율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_RD_MIN_TAX_EXEMPT")
@IdClass(RefRdMinTaxExemptId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefRdMinTaxExempt {

    /** R&D 유형 (PK) */
    @Id
    @Column(name = "rd_type", length = 20, nullable = false)
    private String rdType;

    /** 기업 규모 (PK) */
    @Id
    @Column(name = "corp_size", length = 10, nullable = false)
    private String corpSize;

    /** 면제율 (%) */
    @Column(name = "exempt_rate", precision = 5, scale = 2)
    private BigDecimal exemptRate;
}
