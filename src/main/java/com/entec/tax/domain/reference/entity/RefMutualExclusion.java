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
 * REF_MUTUAL_EXCLUSION 테이블 엔티티.
 * <p>
 * 세액공제·감면 항목 간 중복 적용 배제 규칙을 관리한다.
 * 조항 A와 조항 B의 동시 적용 가능 여부를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_MUTUAL_EXCLUSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefMutualExclusion {

    /** 규칙 ID (PK) */
    @Id
    @Column(name = "rule_id", nullable = false)
    private Integer ruleId;

    /** 조항 A */
    @Column(name = "provision_a", length = 20)
    private String provisionA;

    /** 조항 B */
    @Column(name = "provision_b", length = 20)
    private String provisionB;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;

    /** 동시 적용 허용 여부 */
    @Column(name = "is_allowed")
    private Boolean isAllowed;

    /** 조건 설명 */
    @Column(name = "condition_note", length = 500)
    private String conditionNote;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;
}
