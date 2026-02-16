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
 * REF_SME_DEDUCTION_RATE 테이블 엔티티.
 * <p>
 * 중소기업 특별세액감면율 정보를 관리한다.
 * 기업 규모 세부 분류·업종·권역별 감면율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_SME_DEDUCTION_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefSmeDeductionRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** 기업 규모 세부 분류 (소기업/중기업) */
    @Column(name = "corp_size_detail", length = 10)
    private String corpSizeDetail;

    /** 업종 분류 */
    @Column(name = "industry_class", length = 50)
    private String industryClass;

    /** 권역 유형 (수도권/비수도권) */
    @Column(name = "zone_type", length = 20)
    private String zoneType;

    /** 감면율 (%) */
    @Column(name = "deduction_rate", precision = 5, scale = 2)
    private BigDecimal deductionRate;
}
