package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_COMBINATION 복합 기본키 클래스.
 */
public class OutCombinationId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String comboId;

    public OutCombinationId() {
    }

    public OutCombinationId(String reqId, String comboId) {
        this.reqId = reqId;
        this.comboId = comboId;
    }

    public String getReqId() {
        return reqId;
    }

    public String getComboId() {
        return comboId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutCombinationId that = (OutCombinationId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(comboId, that.comboId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, comboId);
    }
}
