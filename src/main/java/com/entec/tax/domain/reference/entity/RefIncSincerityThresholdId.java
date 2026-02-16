package com.entec.tax.domain.reference.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * REF_INC_SINCERITY_THRESHOLD 복합 기본키 클래스.
 */
public class RefIncSincerityThresholdId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String industryGroup;
    private String effectiveFrom;

    public RefIncSincerityThresholdId() {
    }

    public RefIncSincerityThresholdId(String industryGroup, String effectiveFrom) {
        this.industryGroup = industryGroup;
        this.effectiveFrom = effectiveFrom;
    }

    public String getIndustryGroup() {
        return industryGroup;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefIncSincerityThresholdId that = (RefIncSincerityThresholdId) o;
        return Objects.equals(industryGroup, that.industryGroup)
                && Objects.equals(effectiveFrom, that.effectiveFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(industryGroup, effectiveFrom);
    }
}
