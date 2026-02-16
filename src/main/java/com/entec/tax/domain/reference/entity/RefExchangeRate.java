package com.entec.tax.domain.reference.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * REF_EXCHANGE_RATE 테이블 엔티티.
 * <p>
 * 환율 정보를 관리한다.
 * 일자·통화별 기준환율·매입환율·매도환율을 정의한다.
 * </p>
 */
@Entity
@Table(name = "REF_EXCHANGE_RATE")
@IdClass(RefExchangeRateId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefExchangeRate {

    /** 환율 기준일 (PK) */
    @Id
    @Column(name = "rate_date", nullable = false)
    private Date rateDate;

    /** 통화 코드 (PK, 예: USD, EUR, JPY) */
    @Id
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** 기준환율 */
    @Column(name = "standard_rate", precision = 10, scale = 4)
    private BigDecimal standardRate;

    /** 매입환율 */
    @Column(name = "buy_rate", precision = 10, scale = 4)
    private BigDecimal buyRate;

    /** 매도환율 */
    @Column(name = "sell_rate", precision = 10, scale = 4)
    private BigDecimal sellRate;
}
