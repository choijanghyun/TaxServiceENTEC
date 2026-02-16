package com.entec.tax.domain.request.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * REQ_REQUEST 테이블 엔티티.
 * <p>
 * 세액공제 환급 요청 건의 메타 정보를 관리한다.
 * </p>
 */
@Entity
@Table(name = "REQ_REQUEST")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReqRequest {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 신청자 유형 (C: 법인, P: 개인) */
    @Column(name = "applicant_type", columnDefinition = "CHAR(1)", nullable = false)
    private String applicantType;

    /** 신청자 식별번호 (사업자등록번호/주민등록번호) */
    @Column(name = "applicant_id", length = 15, nullable = false)
    private String applicantId;

    /** 신청자명 (법인명/성명) */
    @Column(name = "applicant_name", length = 100, nullable = false)
    private String applicantName;

    /** 세금 유형 코드 (CORP: 법인세, INC: 종합소득세) */
    @Column(name = "tax_type", length = 4, nullable = false)
    private String taxType;

    /** 귀속 연도 */
    @Column(name = "tax_year", length = 4, nullable = false)
    private String taxYear;

    /** 요청 접수일 */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    /** 동일 신청자 연번 */
    @Column(name = "seq_no")
    private Integer seqNo;

    /** 요청 처리 상태 */
    @Column(name = "request_status", length = 20, nullable = false)
    private String requestStatus;

    /** 프롬프트 버전 */
    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    /** 설계문서 버전 */
    @Column(name = "design_version", length = 20)
    private String designVersion;

    /** 요청 생성 일시 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 처리 완료 일시 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 요청 출처 (API, WEB, BATCH 등) */
    @Column(name = "request_source", length = 30)
    private String requestSource;

    /** 요청자 (사용자 ID 또는 시스템명) */
    @Column(name = "requested_by", length = 50)
    private String requestedBy;

    /** 클라이언트 IP 주소 */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** 오류 메시지 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 수정자 */
    @Column(name = "modified_by", length = 50)
    private String modifiedBy;

    /** 수정 일시 */
    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    /** 낙관적 잠금 버전 */
    @Column(name = "version", nullable = false)
    private Integer version;

    @Builder
    public ReqRequest(String reqId, String applicantType, String applicantId,
                      String applicantName, String taxType, String taxYear,
                      LocalDate requestDate, Integer seqNo, String requestStatus,
                      String promptVersion, String designVersion,
                      LocalDateTime createdAt, LocalDateTime completedAt,
                      String requestSource, String requestedBy, String clientIp,
                      String errorMessage, String modifiedBy, LocalDateTime modifiedAt,
                      Integer version) {
        this.reqId = reqId;
        this.applicantType = applicantType;
        this.applicantId = applicantId;
        this.applicantName = applicantName;
        this.taxType = taxType;
        this.taxYear = taxYear;
        this.requestDate = requestDate;
        this.seqNo = seqNo;
        this.requestStatus = requestStatus;
        this.promptVersion = promptVersion;
        this.designVersion = designVersion;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.requestSource = requestSource;
        this.requestedBy = requestedBy;
        this.clientIp = clientIp;
        this.errorMessage = errorMessage;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
        this.version = version == null ? 1 : version;
    }
}
