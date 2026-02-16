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
import java.util.Date;

/**
 * REF_KSIC_CODE 테이블 엔티티.
 * <p>
 * 한국표준산업분류(KSIC) 코드 정보를 관리한다.
 * 대분류·중분류·소분류·세분류·세세분류별 업종 체계를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_KSIC_CODE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefKsicCode {

    /** KSIC 코드 (PK) */
    @Id
    @Column(name = "ksic_code", length = 10, nullable = false)
    private String ksicCode;

    /** 대분류 (섹션) */
    @Column(name = "section", length = 5)
    private String section;

    /** 중분류 (디비전) */
    @Column(name = "division", length = 5)
    private String division;

    /** 소분류 (그룹) */
    @Column(name = "group_code", length = 5)
    private String groupCode;

    /** 세분류 (클래스) */
    @Column(name = "class_code", length = 5)
    private String classCode;

    /** 세세분류 (서브클래스) */
    @Column(name = "sub_class", length = 5)
    private String subClass;

    /** 업종명 */
    @Column(name = "industry_name", length = 200)
    private String industryName;

    /** 개정 버전 */
    @Column(name = "revision", length = 10)
    private String revision;

    /** 시행일 */
    @Column(name = "effective_date")
    private Date effectiveDate;
}
