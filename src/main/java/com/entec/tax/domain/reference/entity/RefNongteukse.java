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
 * REF_NONGTEUKSE 테이블 엔티티.
 * <p>
 * 농어촌특별세 과세·면제 기준 정보를 관리한다.
 * 조항별 면제 여부 및 세율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_NONGTEUKSE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefNongteukse {

    /** 조항 (PK) */
    @Id
    @Column(name = "provision", length = 20, nullable = false)
    private String provision;

    /** 면제 여부 */
    @Column(name = "is_exempt")
    private Boolean isExempt;

    /** 세율 (%) */
    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;
}
