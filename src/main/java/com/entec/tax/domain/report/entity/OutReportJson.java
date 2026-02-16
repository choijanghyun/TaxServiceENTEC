package com.entec.tax.domain.report.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * OUT_REPORT_JSON 테이블 엔티티 (§26).
 * <p>
 * 최종 보고서 JSON 데이터를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_REPORT_JSON")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutReportJson {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 보고서 버전 */
    @Column(name = "report_version", length = 20)
    private String reportVersion;

    /** 보고서 상태 */
    @Column(name = "report_status", length = 20)
    private String reportStatus;

    /** 보고서 JSON (LONGTEXT) */
    @Lob
    @Column(name = "report_json")
    private String reportJson;

    /** 섹션 A JSON */
    @Column(name = "section_a_json", columnDefinition = "TEXT")
    private String sectionAJson;

    /** 섹션 B JSON */
    @Column(name = "section_b_json", columnDefinition = "TEXT")
    private String sectionBJson;

    /** 섹션 C JSON */
    @Column(name = "section_c_json", columnDefinition = "TEXT")
    private String sectionCJson;

    /** 섹션 D JSON */
    @Column(name = "section_d_json", columnDefinition = "TEXT")
    private String sectionDJson;

    /** 섹션 E JSON */
    @Column(name = "section_e_json", columnDefinition = "TEXT")
    private String sectionEJson;

    /** 섹션 F JSON */
    @Column(name = "section_f_json", columnDefinition = "TEXT")
    private String sectionFJson;

    /** 섹션 G 메타 */
    @Column(name = "section_g_meta", columnDefinition = "TEXT")
    private String sectionGMeta;

    /** JSON 바이트 크기 */
    @Column(name = "json_byte_size")
    private Integer jsonByteSize;

    /** 결과 코드 */
    @Column(name = "result_code", length = 20)
    private String resultCode;

    /** 체크섬 */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /** 생성 일시 */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Builder
    public OutReportJson(String reqId, String reportVersion, String reportStatus,
                          String reportJson, String sectionAJson, String sectionBJson,
                          String sectionCJson, String sectionDJson, String sectionEJson,
                          String sectionFJson, String sectionGMeta, Integer jsonByteSize,
                          String resultCode, String checksum, LocalDateTime generatedAt) {
        this.reqId = reqId;
        this.reportVersion = reportVersion;
        this.reportStatus = reportStatus;
        this.reportJson = reportJson;
        this.sectionAJson = sectionAJson;
        this.sectionBJson = sectionBJson;
        this.sectionCJson = sectionCJson;
        this.sectionDJson = sectionDJson;
        this.sectionEJson = sectionEJson;
        this.sectionFJson = sectionFJson;
        this.sectionGMeta = sectionGMeta;
        this.jsonByteSize = jsonByteSize;
        this.resultCode = resultCode;
        this.checksum = checksum;
        this.generatedAt = generatedAt;
    }
}
