package com.entec.tax.domain.check.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * CHK_VALIDATION_LOG 복합 기본키 클래스.
 */
public class ChkValidationLogId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String ruleCode;

    public ChkValidationLogId() {
    }

    public ChkValidationLogId(String reqId, String ruleCode) {
        this.reqId = reqId;
        this.ruleCode = ruleCode;
    }

    public String getReqId() {
        return reqId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChkValidationLogId that = (ChkValidationLogId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(ruleCode, that.ruleCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, ruleCode);
    }
}
