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
 * REF_DEPOPULATION_AREA 테이블 엔티티.
 * <p>
 * 인구감소지역 지정 정보를 관리한다.
 * 시도·시군구별 지정일 및 활성 상태를 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_DEPOPULATION_AREA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDepopulationArea {

    /** 지역 ID (PK) */
    @Id
    @Column(name = "area_id", nullable = false)
    private Integer areaId;

    /** 시도 */
    @Column(name = "sido", length = 20)
    private String sido;

    /** 시군구 */
    @Column(name = "sigungu", length = 50)
    private String sigungu;

    /** 지정일 */
    @Column(name = "designation_date")
    private Date designationDate;

    /** 유효 시작일 */
    @Column(name = "effective_from")
    private Date effectiveFrom;

    /** 활성 여부 */
    @Column(name = "is_active")
    private Boolean isActive;
}
