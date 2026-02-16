package com.entec.tax.domain.check.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * CHK_VALIDATION_LOG 테이블 엔티티 (§17).
 * <p>
 * 검증 규칙별 실행 결과 로그를 관리한다.
 * </p>
 */
@Entity
@Table(name = "CHK_VALIDATION_LOG")
@IdClass(ChkValidationLogId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChkValidationLog {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 규칙 코드 (PK) */
    @Id
    @Column(name = "rule_code", length = 30, nullable = false)
    private String ruleCode;

    /** 규칙 유형 */
    @Column(name = "rule_type", columnDefinition = "CHAR(1)")
    private String ruleType;

    /** 규칙 설명 */
    @Column(name = "rule_description", length = 500)
    private String ruleDescription;

    /** 결과 (PASS/FAIL/WARNING/SKIP) */
    @Column(name = "result", length = 10)
    private String result;

    /** 상세 내용 */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 실행 일시 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Builder
    public ChkValidationLog(String reqId, String ruleCode, String ruleType,
                             String ruleDescription, String result,
                             String detail, LocalDateTime executedAt) {
        this.reqId = reqId;
        this.ruleCode = ruleCode;
        this.ruleType = ruleType;
        this.ruleDescription = ruleDescription;
        this.result = result;
        this.detail = detail;
        this.executedAt = executedAt;
    }
}
