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

/**
 * REF_INC_DEDUCTION_LIMIT 테이블 엔티티.
 * <p>
 * 개인(소득세) 소득공제 한도 정보를 관리한다.
 * 공제 유형·소득 구간별 연간 한도액을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INC_DEDUCTION_LIMIT")
@IdClass(RefIncDeductionLimitId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefIncDeductionLimit {

    /** 공제 유형 (PK) */
    @Id
    @Column(name = "deduction_type", length = 50, nullable = false)
    private String deductionType;

    /** 소득 구간 (PK) */
    @Id
    @Column(name = "income_bracket", length = 50, nullable = false)
    private String incomeBracket;

    /** 연간 한도액 */
    @Column(name = "annual_limit")
    private Long annualLimit;
}
