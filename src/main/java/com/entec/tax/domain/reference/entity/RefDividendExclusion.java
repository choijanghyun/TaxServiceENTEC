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
import java.math.BigDecimal;

/**
 * REF_DIVIDEND_EXCLUSION 테이블 엔티티.
 * <p>
 * 수입배당금 익금불산입률 정보를 관리한다.
 * 법인 유형·지분율 구간별 익금불산입률을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_DIVIDEND_EXCLUSION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefDividendExclusion {

    /** 배제 ID (PK) */
    @Id
    @Column(name = "exclusion_id", nullable = false)
    private Integer exclusionId;

    /** 적용 시작 연도 */
    @Column(name = "year_from", length = 4)
    private String yearFrom;

    /** 적용 종료 연도 */
    @Column(name = "year_to", length = 4)
    private String yearTo;

    /** 법인 유형 (상장/비상장 등) */
    @Column(name = "corp_type", length = 20)
    private String corpType;

    /** 지분율 하한 (%) */
    @Column(name = "share_ratio_min", precision = 5, scale = 2)
    private BigDecimal shareRatioMin;

    /** 지분율 상한 (%) */
    @Column(name = "share_ratio_max", precision = 5, scale = 2)
    private BigDecimal shareRatioMax;

    /** 익금불산입률 (%) */
    @Column(name = "exclusion_rate", precision = 5, scale = 2)
    private BigDecimal exclusionRate;

    /** 비고 */
    @Column(name = "remark", length = 200)
    private String remark;
}
