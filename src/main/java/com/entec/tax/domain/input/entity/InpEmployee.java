package com.entec.tax.domain.input.entity;

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
 * INP_EMPLOYEE 테이블 엔티티.
 * <p>
 * 요청 건의 고용 인원 및 급여 정보를 연도 구분별로 저장한다.
 * 설계문서 section 11 기반.
 * </p>
 */
@Entity
@Table(name = "INP_EMPLOYEE")
@IdClass(InpEmployeeId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InpEmployee {

    /** 요청 ID (복합 PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 연도 구분 (복합 PK: CURRENT, PREV1, PREV2 등) */
    @Id
    @Column(name = "year_type", length = 10, nullable = false)
    private String yearType;

    /** 상시근로자 수 (소수점 포함 가능) */
    @Column(name = "total_regular", precision = 10, scale = 2)
    private BigDecimal totalRegular;

    /** 청년등 근로자 수 */
    @Column(name = "youth_count")
    private Integer youthCount;

    /** 장애인 근로자 수 */
    @Column(name = "disabled_count")
    private Integer disabledCount;

    /** 고령자 근로자 수 */
    @Column(name = "aged_count")
    private Integer agedCount;

    /** 경력단절여성 근로자 수 */
    @Column(name = "career_break_count")
    private Integer careerBreakCount;

    /** 북한이탈주민 근로자 수 */
    @Column(name = "north_defector_count")
    private Integer northDefectorCount;

    /** 일반 근로자 수 */
    @Column(name = "general_count")
    private Integer generalCount;

    /** 제외 근로자 수 */
    @Column(name = "excluded_count")
    private Integer excludedCount;

    /** 총 급여액 (원) */
    @Column(name = "total_salary")
    private Long totalSalary;

    /** 사회보험료 납부액 (원) */
    @Column(name = "social_insurance_paid")
    private Long socialInsurancePaid;

    @Builder
    public InpEmployee(String reqId, String yearType, BigDecimal totalRegular,
                       Integer youthCount, Integer disabledCount, Integer agedCount,
                       Integer careerBreakCount, Integer northDefectorCount,
                       Integer generalCount, Integer excludedCount,
                       Long totalSalary, Long socialInsurancePaid) {
        this.reqId = reqId;
        this.yearType = yearType;
        this.totalRegular = totalRegular;
        this.youthCount = youthCount;
        this.disabledCount = disabledCount;
        this.agedCount = agedCount;
        this.careerBreakCount = careerBreakCount;
        this.northDefectorCount = northDefectorCount;
        this.generalCount = generalCount;
        this.excludedCount = excludedCount;
        this.totalSalary = totalSalary;
        this.socialInsurancePaid = socialInsurancePaid;
    }
}
