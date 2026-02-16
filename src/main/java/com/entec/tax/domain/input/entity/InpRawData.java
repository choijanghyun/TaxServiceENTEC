package com.entec.tax.domain.input.entity;

import com.entec.tax.domain.request.entity.ReqRequest;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * INP_RAW_DATA 테이블 엔티티.
 * <p>
 * 요청 건에 대한 원본(Raw) JSON 데이터를 저장한다.
 * 카테고리별로 분류하여 원본 데이터를 보관한다.
 * </p>
 */
@Entity
@Table(name = "INP_RAW_DATA")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InpRawData {

    /** 원본 데이터 ID (PK, 자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_id", nullable = false)
    private Long rawId;

    /** 요청 엔티티 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "req_id", nullable = false)
    private ReqRequest reqRequest;

    /** 데이터 카테고리 (BASIC, EMPLOYEE, DEDUCTION 등) */
    @Column(name = "category", length = 30, nullable = false)
    private String category;

    /** 데이터 서브 카테고리 */
    @Column(name = "sub_category", length = 30)
    private String subCategory;

    /** 원본 JSON 데이터 */
    @Lob
    @Column(name = "raw_json", nullable = false)
    private String rawJson;

    /** JSON 스키마 버전 */
    @Column(name = "json_schema_version", length = 20)
    private String jsonSchemaVersion;

    /** 레코드 건수 */
    @Column(name = "record_count")
    private Integer recordCount;

    /** 데이터 크기 (bytes) */
    @Column(name = "byte_size")
    private Long byteSize;

    /** 데이터 체크섬 (무결성 검증용) */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /** 수신 일시 */
    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Builder
    public InpRawData(Long rawId, ReqRequest reqRequest, String category,
                      String subCategory, String rawJson, String jsonSchemaVersion,
                      Integer recordCount, Long byteSize, String checksum,
                      LocalDateTime receivedAt) {
        this.rawId = rawId;
        this.reqRequest = reqRequest;
        this.category = category;
        this.subCategory = subCategory;
        this.rawJson = rawJson;
        this.jsonSchemaVersion = jsonSchemaVersion;
        this.recordCount = recordCount;
        this.byteSize = byteSize;
        this.checksum = checksum;
        this.receivedAt = receivedAt;
    }
}
