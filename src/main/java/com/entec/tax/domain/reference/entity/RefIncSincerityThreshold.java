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
 * REF_INC_SINCERITY_THRESHOLD 테이블 엔티티.
 * <p>
 * 개인(소득세) 성실신고확인 대상 수입금액 기준을 관리한다.
 * 업종군·적용 시작 연도별 수입금액 기준을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_INC_SINCERITY_THRESHOLD")
@IdClass(RefIncSincerityThresholdId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefIncSincerityThreshold {

    /** 업종군 (PK) */
    @Id
    @Column(name = "industry_group", length = 50, nullable = false)
    private String industryGroup;

    /** 적용 시작 연도 (PK) */
    @Id
    @Column(name = "effective_from", length = 4, nullable = false)
    private String effectiveFrom;

    /** 수입금액 기준 */
    @Column(name = "revenue_threshold")
    private Long revenueThreshold;
}
