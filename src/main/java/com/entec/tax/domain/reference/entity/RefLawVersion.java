package com.entec.tax.domain.reference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * REF_LAW_VERSION 테이블 엔티티.
 * <p>
 * 조세특례제한법 등 관련 법령 버전 이력을 관리한다.
 * 조항별 적용 기간 및 개정 내역을 기록한다.
 * </p>
 */
@Entity
@Table(name = "REF_LAW_VERSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefLawVersion {

    /** 버전 ID (PK) */
    @Id
    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    /** 법률명 */
    @Column(name = "law_name", length = 100)
    private String lawName;

    /** 조항 */
    @Column(name = "provision", length = 20)
    private String provision;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;

    /** 버전 설명 */
    @Column(name = "version_note", columnDefinition = "TEXT")
    private String versionNote;
}
