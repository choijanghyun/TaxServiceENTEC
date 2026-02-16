package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_ADDITIONAL_CHECK 복합 기본키 클래스.
 */
public class OutAdditionalCheckId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String checkId;

    public OutAdditionalCheckId() {
    }

    public OutAdditionalCheckId(String reqId, String checkId) {
        this.reqId = reqId;
        this.checkId = checkId;
    }

    public String getReqId() {
        return reqId;
    }

    public String getCheckId() {
        return checkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutAdditionalCheckId that = (OutAdditionalCheckId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(checkId, that.checkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, checkId);
    }
}
