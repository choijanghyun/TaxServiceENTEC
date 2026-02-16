package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_RISK 복합 기본키 클래스.
 */
public class OutRiskId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String riskId;

    public OutRiskId() {
    }

    public OutRiskId(String reqId, String riskId) {
        this.reqId = reqId;
        this.riskId = riskId;
    }

    public String getReqId() {
        return reqId;
    }

    public String getRiskId() {
        return riskId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutRiskId that = (OutRiskId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(riskId, that.riskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, riskId);
    }
}
