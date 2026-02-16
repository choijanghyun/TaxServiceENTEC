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
import java.util.Date;

/**
 * REF_REFUND_INTEREST_RATE 테이블 엔티티.
 * <p>
 * 환급 가산금 이자율 정보를 관리한다.
 * 유효 기간별 연 이자율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_REFUND_INTEREST_RATE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefRefundInterestRate {

    /** 세율 ID (PK) */
    @Id
    @Column(name = "rate_id", nullable = false)
    private Integer rateId;

    /** 유효 시작일 */
    @Column(name = "effective_from")
    private Date effectiveFrom;

    /** 유효 종료일 */
    @Column(name = "effective_to")
    private Date effectiveTo;

    /** 연 이자율 */
    @Column(name = "annual_rate", precision = 7, scale = 5)
    private BigDecimal annualRate;

    /** 법적 근거 */
    @Column(name = "legal_basis", length = 100)
    private String legalBasis;
}
