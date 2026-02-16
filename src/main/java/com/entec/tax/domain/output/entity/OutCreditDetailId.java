package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_CREDIT_DETAIL 복합 기본키 클래스.
 */
public class OutCreditDetailId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String itemId;

    public OutCreditDetailId() {
    }

    public OutCreditDetailId(String reqId, String itemId) {
        this.reqId = reqId;
        this.itemId = itemId;
    }

    public String getReqId() {
        return reqId;
    }

    public String getItemId() {
        return itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutCreditDetailId that = (OutCreditDetailId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, itemId);
    }
}
