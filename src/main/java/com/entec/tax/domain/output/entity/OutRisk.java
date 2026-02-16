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
import java.time.LocalDate;

/**
 * OUT_RISK 테이블 엔티티 (§23).
 * <p>
 * 세액공제 관련 위험 항목을 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_RISK")
@IdClass(OutRiskId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutRisk {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 위험 ID (PK) */
    @Id
    @Column(name = "risk_id", length = 30, nullable = false)
    private String riskId;

    /** 조항 */
    @Column(name = "provision", length = 50)
    private String provision;

    /** 위험 유형 */
    @Column(name = "risk_type", length = 30)
    private String riskType;

    /** 의무 사항 */
    @Column(name = "obligation", length = 200)
    private String obligation;

    /** 기간 시작일 */
    @Column(name = "period_start")
    private LocalDate periodStart;

    /** 기간 종료일 */
    @Column(name = "period_end", length = 20)
    private String periodEnd;

    /** 위반 시 조치 */
    @Column(name = "violation_action", length = 200)
    private String violationAction;

    /** 잠재적 환수 금액 */
    @Column(name = "potential_clawback")
    private Long potentialClawback;

    /** 이자 가산세 */
    @Column(name = "interest_surcharge")
    private Long interestSurcharge;

    /** 위험 수준 */
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    /** 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder
    public OutRisk(String reqId, String riskId, String provision,
                   String riskType, String obligation, LocalDate periodStart,
                   String periodEnd, String violationAction,
                   Long potentialClawback, Long interestSurcharge,
                   String riskLevel, String description) {
        this.reqId = reqId;
        this.riskId = riskId;
        this.provision = provision;
        this.riskType = riskType;
        this.obligation = obligation;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.violationAction = violationAction;
        this.potentialClawback = potentialClawback;
        this.interestSurcharge = interestSurcharge;
        this.riskLevel = riskLevel;
        this.description = description;
    }
}
