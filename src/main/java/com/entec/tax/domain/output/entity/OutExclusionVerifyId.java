package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_EXCLUSION_VERIFY 복합 기본키 클래스.
 */
public class OutExclusionVerifyId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String verifyId;

    public OutExclusionVerifyId() {
    }

    public OutExclusionVerifyId(String reqId, String verifyId) {
        this.reqId = reqId;
        this.verifyId = verifyId;
    }

    public String getReqId() {
        return reqId;
    }

    public String getVerifyId() {
        return verifyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutExclusionVerifyId that = (OutExclusionVerifyId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(verifyId, that.verifyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, verifyId);
    }
}
