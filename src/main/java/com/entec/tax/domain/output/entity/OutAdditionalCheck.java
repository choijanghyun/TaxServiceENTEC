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
 * OUT_ADDITIONAL_CHECK 테이블 엔티티 (§24).
 * <p>
 * 추가 확인 필요 항목을 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_ADDITIONAL_CHECK")
@IdClass(OutAdditionalCheckId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutAdditionalCheck {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 체크 ID (PK) */
    @Id
    @Column(name = "check_id", length = 30, nullable = false)
    private String checkId;

    /** 설명 */
    @Column(name = "description", length = 500)
    private String description;

    /** 사유 */
    @Column(name = "reason", length = 500)
    private String reason;

    /** 관련 점검 항목 */
    @Column(name = "related_inspection", length = 50)
    private String relatedInspection;

    /** 관련 모듈 */
    @Column(name = "related_module", length = 50)
    private String relatedModule;

    /** 우선순위 */
    @Column(name = "priority", length = 10)
    private String priority;

    /** 상태 */
    @Column(name = "status", length = 20)
    private String status;

    @Builder
    public OutAdditionalCheck(String reqId, String checkId, String description,
                               String reason, String relatedInspection,
                               String relatedModule, String priority, String status) {
        this.reqId = reqId;
        this.checkId = checkId;
        this.description = description;
        this.reason = reason;
        this.relatedInspection = relatedInspection;
        this.relatedModule = relatedModule;
        this.priority = priority;
        this.status = status;
    }
}
