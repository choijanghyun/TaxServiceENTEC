package com.entec.tax.domain.input.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * INP_DEDUCTION 복합 기본키 클래스.
 * <p>
 * reqId (요청 ID) + itemCategory (항목 카테고리) + provision (조항) + taxYear (귀속연도) + itemSeq (항목 순번) 으로 구성된다.
 * </p>
 */
public class InpDeductionId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 요청 ID */
    private String reqId;

    /** 항목 카테고리 (INVEST, RD, EMPLOYMENT 등) */
    private String itemCategory;

    /** 조세특례제한법 조항 */
    private String provision;

    /** 귀속 연도 */
    private String taxYear;

    /** 항목 순번 */
    private Integer itemSeq;

    public InpDeductionId() {
    }

    public InpDeductionId(String reqId, String itemCategory, String provision,
                          String taxYear, Integer itemSeq) {
        this.reqId = reqId;
        this.itemCategory = itemCategory;
        this.provision = provision;
        this.taxYear = taxYear;
        this.itemSeq = itemSeq;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public String getProvision() {
        return provision;
    }

    public void setProvision(String provision) {
        this.provision = provision;
    }

    public String getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(String taxYear) {
        this.taxYear = taxYear;
    }

    public Integer getItemSeq() {
        return itemSeq;
    }

    public void setItemSeq(Integer itemSeq) {
        this.itemSeq = itemSeq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InpDeductionId that = (InpDeductionId) o;
        return Objects.equals(reqId, that.reqId)
                && Objects.equals(itemCategory, that.itemCategory)
                && Objects.equals(provision, that.provision)
                && Objects.equals(taxYear, that.taxYear)
                && Objects.equals(itemSeq, that.itemSeq);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqId, itemCategory, provision, taxYear, itemSeq);
    }
}
