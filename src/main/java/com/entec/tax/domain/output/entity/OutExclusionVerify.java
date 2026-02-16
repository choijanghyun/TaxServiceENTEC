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

/**
 * OUT_EXCLUSION_VERIFY 테이블 엔티티 (§21).
 * <p>
 * 중복 적용 배제 검증 결과를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_EXCLUSION_VERIFY")
@IdClass(OutExclusionVerifyId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutExclusionVerify {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 검증 ID (PK) */
    @Id
    @Column(name = "verify_id", length = 30, nullable = false)
    private String verifyId;

    /** 조합 ID */
    @Column(name = "combo_id", length = 30)
    private String comboId;

    /** 조항 A */
    @Column(name = "provision_a", length = 50)
    private String provisionA;

    /** 조항 B */
    @Column(name = "provision_b", length = 50)
    private String provisionB;

    /** 중복 적용 허용 여부 */
    @Column(name = "overlap_allowed", length = 10)
    private String overlapAllowed;

    /** 조건 비고 */
    @Column(name = "condition_note", length = 500)
    private String conditionNote;

    /** 위반 감지 여부 */
    @Column(name = "violation_detected")
    private Boolean violationDetected;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    @Builder
    public OutExclusionVerify(String reqId, String verifyId, String comboId,
                               String provisionA, String provisionB,
                               String overlapAllowed, String conditionNote,
                               Boolean violationDetected, String legalBasis) {
        this.reqId = reqId;
        this.verifyId = verifyId;
        this.comboId = comboId;
        this.provisionA = provisionA;
        this.provisionB = provisionB;
        this.overlapAllowed = overlapAllowed;
        this.conditionNote = conditionNote;
        this.violationDetected = violationDetected;
        this.legalBasis = legalBasis;
    }
}
