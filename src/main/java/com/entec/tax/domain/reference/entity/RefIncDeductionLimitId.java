package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * REF_INC_DEDUCTION_LIMIT 복합 기본키 클래스.
 */
public class RefIncDeductionLimitId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deductionType;
    private String incomeBracket;

    public RefIncDeductionLimitId() {
    }

    public RefIncDeductionLimitId(String deductionType, String incomeBracket) {
        this.deductionType = deductionType;
        this.incomeBracket = incomeBracket;
    }

    public String getDeductionType() {
        return deductionType;
    }

    public String getIncomeBracket() {
        return incomeBracket;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefIncDeductionLimitId that = (RefIncDeductionLimitId) o;
        return Objects.equals(deductionType, that.deductionType)
                && Objects.equals(incomeBracket, that.incomeBracket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deductionType, incomeBracket);
    }
}
