package com.entec.tax.domain.output.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * OUT_EMPLOYEE_SUMMARY 복합 기본키 클래스.
 */
public class OutEmployeeSummaryId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reqId;
    private String yearType;

    public OutEmployeeSummaryId() {
    }

    public OutEmployeeSummaryId(String reqId, String yearType) {
        this.reqId = reqId;
        this.yearType = yearType;
    }

    public String getReqId() {
        return reqId;
    }

    public String getYearType() {
        return yearType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutEmployeeSummaryId that = (OutEmployeeSummaryId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(yearType, that.yearType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, yearType);
    }
}
