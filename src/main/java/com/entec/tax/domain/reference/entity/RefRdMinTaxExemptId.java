package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * REF_RD_MIN_TAX_EXEMPT 복합 기본키 클래스.
 */
public class RefRdMinTaxExemptId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rdType;
    private String corpSize;

    public RefRdMinTaxExemptId() {
    }

    public RefRdMinTaxExemptId(String rdType, String corpSize) {
        this.rdType = rdType;
        this.corpSize = corpSize;
    }

    public String getRdType() {
        return rdType;
    }

    public String getCorpSize() {
        return corpSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefRdMinTaxExemptId that = (RefRdMinTaxExemptId) o;
        return Objects.equals(rdType, that.rdType)
                && Objects.equals(corpSize, that.corpSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rdType, corpSize);
    }
}
