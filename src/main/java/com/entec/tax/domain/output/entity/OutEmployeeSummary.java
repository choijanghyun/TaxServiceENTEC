package com.entec.tax.domain.output.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * OUT_EMPLOYEE_SUMMARY 테이블 엔티티 (§18).
 * <p>
 * 고용 인원 요약 정보를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_EMPLOYEE_SUMMARY")
@IdClass(OutEmployeeSummaryId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutEmployeeSummary {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 연도 유형 (PK) */
    @Id
    @Column(name = "year_type", length = 20, nullable = false)
    private String yearType;

    /** 상시 근로자 합계 */
    @Column(name = "total_regular", precision = 15, scale = 2)
    private BigDecimal totalRegular;

    /** 청년 인원 수 */
    @Column(name = "youth_count")
    private Integer youthCount;

    /** 일반 인원 수 */
    @Column(name = "general_count")
    private Integer generalCount;

    /** 증가 인원 합계 */
    @Column(name = "increase_total")
    private Integer increaseTotal;

    /** 청년 증가 인원 */
    @Column(name = "increase_youth")
    private Integer increaseYouth;

    /** 일반 증가 인원 */
    @Column(name = "increase_general")
    private Integer increaseGeneral;

    /** 제외 인원 수 */
    @Column(name = "excluded_count")
    private Integer excludedCount;

    /** 산출 상세 */
    @Column(name = "calc_detail", columnDefinition = "TEXT")
    private String calcDetail;

    @Builder
    public OutEmployeeSummary(String reqId, String yearType, BigDecimal totalRegular,
                               Integer youthCount, Integer generalCount,
                               Integer increaseTotal, Integer increaseYouth,
                               Integer increaseGeneral, Integer excludedCount,
                               String calcDetail) {
        this.reqId = reqId;
        this.yearType = yearType;
        this.totalRegular = totalRegular;
        this.youthCount = youthCount;
        this.generalCount = generalCount;
        this.increaseTotal = increaseTotal;
        this.increaseYouth = increaseYouth;
        this.increaseGeneral = increaseGeneral;
        this.excludedCount = excludedCount;
        this.calcDetail = calcDetail;
    }
}
