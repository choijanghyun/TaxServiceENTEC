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
 * REF_CAPITAL_ZONE 테이블 엔티티.
 * <p>
 * 수도권 과밀억제권역·성장관리권역·자연보전권역 등 지역 구분 정보를 관리한다.
 * 시도·시군구별 수도권 여부 및 인구감소지역 여부를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_CAPITAL_ZONE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefCapitalZone {

    /** 지역 ID (PK) */
    @Id
    @Column(name = "zone_id", nullable = false)
    private Integer zoneId;

    /** 시도 */
    @Column(name = "sido", length = 20)
    private String sido;

    /** 시군구 */
    @Column(name = "sigungu", length = 50)
    private String sigungu;

    /** 권역 유형 (과밀억제/성장관리/자연보전/비수도권) */
    @Column(name = "zone_type", length = 20)
    private String zoneType;

    /** 수도권 여부 */
    @Column(name = "is_capital")
    private Boolean isCapital;

    /** 인구감소지역 여부 */
    @Column(name = "is_depopulation")
    private Boolean isDepopulation;
}
