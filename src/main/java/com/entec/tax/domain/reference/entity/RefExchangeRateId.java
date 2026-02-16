package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * REF_EXCHANGE_RATE 복합 기본키 클래스.
 */
public class RefExchangeRateId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date rateDate;
    private String currency;

    public RefExchangeRateId() {
    }

    public RefExchangeRateId(Date rateDate, String currency) {
        this.rateDate = rateDate;
        this.currency = currency;
    }

    public Date getRateDate() {
        return rateDate;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefExchangeRateId that = (RefExchangeRateId) o;
        return Objects.equals(rateDate, that.rateDate)
                && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rateDate, currency);
    }
}
