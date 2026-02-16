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
 * REF_SYSTEM_PARAM 테이블 엔티티.
 * <p>
 * 시스템 파라미터 정보를 관리한다.
 * 세무 계산에 필요한 각종 설정값을 key-value 형태로 저장한다.
 * </p>
 */
@Entity
@Table(name = "REF_SYSTEM_PARAM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefSystemParam {

    /** 파라미터 키 (PK) */
    @Id
    @Column(name = "param_key", length = 50, nullable = false)
    private String paramKey;

    /** 파라미터 값 */
    @Column(name = "param_value", length = 100)
    private String paramValue;

    /** 파라미터 유형 */
    @Column(name = "param_type", length = 20)
    private String paramType;

    /** 설명 */
    @Column(name = "description", length = 200)
    private String description;

    /** 수정 가능 여부 */
    @Column(name = "modifiable")
    private Boolean modifiable;

    /** 최종 수정일 */
    @Column(name = "last_updated")
    private Date lastUpdated;
}
