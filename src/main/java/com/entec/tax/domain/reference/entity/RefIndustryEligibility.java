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
import java.util.Date;

/**
 * REF_INDUSTRY_ELIGIBILITY 테이블 엔티티.
 * <p>
 * 업종별 세제 혜택 적격 여부를 관리한다.
 * KSIC 코드별 창업감면·중소기업 특별세액감면 등의 적격 여부를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INDUSTRY_ELIGIBILITY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefIndustryEligibility {

    /** KSIC 코드 (PK) */
    @Id
    @Column(name = "ksic_code", length = 10, nullable = false)
    private String ksicCode;

    /** 업종명 */
    @Column(name = "industry_name", length = 100)
    private String industryName;

    /** 창업감면 적격 여부 */
    @Column(name = "startup_eligible")
    private Boolean startupEligible;

    /** 중소기업 특별세액감면 적격 여부 */
    @Column(name = "sme_special_eligible")
    private Boolean smeSpecialEligible;

    /** 제외 사유 */
    @Column(name = "excluded_reason", length = 200)
    private String excludedReason;

    /** 유효 시작일 */
    @Column(name = "effective_from")
    private Date effectiveFrom;

    /** 유효 종료일 */
    @Column(name = "effective_to")
    private Date effectiveTo;

    /** 중소기업 적격 여부 */
    @Column(name = "is_sme_eligible")
    private Boolean isSmeEligible;
}
