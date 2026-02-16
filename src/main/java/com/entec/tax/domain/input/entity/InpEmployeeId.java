package com.entec.tax.domain.input.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * INP_EMPLOYEE 복합 기본키 클래스.
 * <p>
 * reqId (요청 ID) + yearType (연도 구분) 으로 구성된다.
 * </p>
 */
public class InpEmployeeId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 요청 ID */
    private String reqId;

    /** 연도 구분 (CURRENT: 당기, PREV1: 직전, PREV2: 직전전 등) */
    private String yearType;

    public InpEmployeeId() {
    }

    public InpEmployeeId(String reqId, String yearType) {
        this.reqId = reqId;
        this.yearType = yearType;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getYearType() {
        return yearType;
    }

    public void setYearType(String yearType) {
        this.yearType = yearType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InpEmployeeId that = (InpEmployeeId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(yearType, that.yearType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, yearType);
    }
}
