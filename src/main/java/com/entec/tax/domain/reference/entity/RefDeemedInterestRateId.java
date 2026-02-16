package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * REF_DEEMED_INTEREST_RATE 복합 기본키 클래스.
 */
public class RefDeemedInterestRateId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String year;
    private String rateType;

    public RefDeemedInterestRateId() {
    }

    public RefDeemedInterestRateId(String year, String rateType) {
        this.year = year;
        this.rateType = rateType;
    }

    public String getYear() {
        return year;
    }

    public String getRateType() {
        return rateType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefDeemedInterestRateId that = (RefDeemedInterestRateId) o;
        return Objects.equals(year, that.year)
                && Objects.equals(rateType, that.rateType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, rateType);
    }
}
