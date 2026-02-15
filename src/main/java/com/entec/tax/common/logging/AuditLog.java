package com.entec.tax.common.logging;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 감사 로그 엔티티
 * - 주요 업무 처리 이력을 데이터베이스에 기록
 * - 사용자의 데이터 조회/등록/수정/삭제 이력 추적
 */
@Entity
@Table(name = "tb_audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    /** 사용자 ID */
    @Column(name = "user_id", length = 50)
    private String userId;

    /** 사용자명 */
    @Column(name = "user_name", length = 100)
    private String userName;

    /** 작업 유형 (CREATE, READ, UPDATE, DELETE) */
    @Column(name = "action_type", length = 20, nullable = false)
    private String actionType;

    /** 대상 테이블명 */
    @Column(name = "target_table", length = 100)
    private String targetTable;

    /** 대상 데이터 ID */
    @Column(name = "target_id", length = 100)
    private String targetId;

    /** 작업 내용 설명 */
    @Column(name = "description", length = 500)
    private String description;

    /** 변경 전 데이터 (JSON) */
    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    /** 변경 후 데이터 (JSON) */
    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    /** 요청 IP 주소 */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** 요청 URL */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /** HTTP 메소드 */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /** 처리 결과 (SUCCESS, FAIL) */
    @Column(name = "result_status", length = 10)
    private String resultStatus;

    /** 에러 메시지 (실패 시) */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /** 작업 일시 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public void setBeforeData(String beforeData) {
        this.beforeData = beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public void setAfterData(String afterData) {
        this.afterData = afterData;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
