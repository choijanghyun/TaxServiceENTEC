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

/**
 * REF_EMPLOYMENT_CREDIT 테이블 엔티티.
 * <p>
 * 고용 관련 세액공제 기준 정보를 관리한다.
 * 기업 규모·지역·근로자 유형별 1인당 공제액을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_EMPLOYMENT_CREDIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefEmploymentCredit {

    /** 공제 ID (PK) */
    @Id
    @Column(name = "credit_id", nullable = false)
    private Integer creditId;

    /** 귀속 연도 */
    @Column(name = "tax_year", length = 4)
    private String taxYear;

    /** 기업 규모 */
    @Column(name = "corp_size", length = 10)
    private String corpSize;

    /** 지역 (수도권/비수도권) */
    @Column(name = "region", length = 10)
    private String region;

    /** 근로자 유형 (청년·장애인·고령자 등) */
    @Column(name = "worker_type", length = 20)
    private String workerType;

    /** 1인당 공제 금액 */
    @Column(name = "credit_per_person")
    private Long creditPerPerson;
}
