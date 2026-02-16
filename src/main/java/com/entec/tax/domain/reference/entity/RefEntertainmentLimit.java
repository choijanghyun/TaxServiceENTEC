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
 * REF_ENTERTAINMENT_LIMIT 테이블 엔티티.
 * <p>
 * 접대비 한도액 산정 기준 정보를 관리한다.
 * 기업 규모·매출 구간별 기본 한도액 및 적용 비율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_ENTERTAINMENT_LIMIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefEntertainmentLimit {

    /** 한도 ID (PK) */
    @Id
    @Column(name = "limit_id", nullable = false)
    private Integer limitId;

    /** 기업 규모 */
    @Column(name = "corp_size", length = 10)
    private String corpSize;

    /** 기본 한도액 */
    @Column(name = "base_amount")
    private Long baseAmount;

    /** 매출 구간 하한 */
    @Column(name = "revenue_bracket_min")
    private Long revenueBracketMin;

    /** 매출 구간 상한 */
    @Column(name = "revenue_bracket_max")
    private Long revenueBracketMax;

    /** 적용 비율 */
    @Column(name = "rate", precision = 5, scale = 4)
    private BigDecimal rate;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;
}
