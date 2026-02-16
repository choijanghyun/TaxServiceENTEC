package com.entec.tax.domain.log.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * LOG_CALCULATION 테이블 엔티티 (§25).
 * <p>
 * 계산 단계별 실행 로그를 관리한다.
 * </p>
 */
@Entity
@Table(name = "LOG_CALCULATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogCalculation {

    /** 로그 ID (PK, 자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Long logId;

    /** 요청 ID (FK) */
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 계산 단계 */
    @Column(name = "calc_step", length = 50)
    private String calcStep;

    /** 함수명 */
    @Column(name = "function_name", length = 100)
    private String functionName;

    /** 입력 데이터 */
    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    /** 출력 데이터 */
    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 200)
    private String legalBasis;

    /** 실행 일시 */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /** 로그 레벨 */
    @Column(name = "log_level", length = 10)
    private String logLevel;

    /** 실행자 */
    @Column(name = "executed_by", length = 50)
    private String executedBy;

    /** 실행 소요 시간 (밀리초) */
    @Column(name = "duration_ms")
    private Integer durationMs;

    /** 추적 ID */
    @Column(name = "trace_id", length = 50)
    private String traceId;

    /** 이전 데이터 해시 */
    @Column(name = "prev_data_hash", length = 64)
    private String prevDataHash;

    @Builder
    public LogCalculation(Long logId, String reqId, String calcStep,
                           String functionName, String inputData, String outputData,
                           String legalBasis, LocalDateTime executedAt, String logLevel,
                           String executedBy, Integer durationMs, String traceId,
                           String prevDataHash) {
        this.logId = logId;
        this.reqId = reqId;
        this.calcStep = calcStep;
        this.functionName = functionName;
        this.inputData = inputData;
        this.outputData = outputData;
        this.legalBasis = legalBasis;
        this.executedAt = executedAt;
        this.logLevel = logLevel;
        this.executedBy = executedBy;
        this.durationMs = durationMs;
        this.traceId = traceId;
        this.prevDataHash = prevDataHash;
    }
}
