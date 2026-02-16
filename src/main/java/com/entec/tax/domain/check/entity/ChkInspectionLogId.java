package com.entec.tax.domain.check.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * CHK_INSPECTION_LOG 복합 기본키 클래스.
 */
public class ChkInspectionLogId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String inspectionCode;

    public ChkInspectionLogId() {
    }

    public ChkInspectionLogId(String reqId, String inspectionCode) {
        this.reqId = reqId;
        this.inspectionCode = inspectionCode;
    }

    public String getReqId() {
        return reqId;
    }

    public String getInspectionCode() {
        return inspectionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChkInspectionLogId that = (ChkInspectionLogId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(inspectionCode, that.inspectionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, inspectionCode);
    }
}
