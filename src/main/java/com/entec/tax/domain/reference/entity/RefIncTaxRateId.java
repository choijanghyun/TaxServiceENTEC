package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * REF_INC_TAX_RATE 복합 기본키 클래스.
 */
public class RefIncTaxRateId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String effectiveFrom;
    private Integer bracketNo;

    public RefIncTaxRateId() {
    }

    public RefIncTaxRateId(String effectiveFrom, Integer bracketNo) {
        this.effectiveFrom = effectiveFrom;
        this.bracketNo = bracketNo;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public Integer getBracketNo() {
        return bracketNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefIncTaxRateId that = (RefIncTaxRateId) o;
        return Objects.equals(effectiveFrom, that.effectiveFrom)
                && Objects.equals(bracketNo, that.bracketNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effectiveFrom, bracketNo);
    }
}
