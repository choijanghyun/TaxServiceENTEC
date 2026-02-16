package com.entec.tax.domain.input.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * INP_DEDUCTION 테이블 엔티티.
 * <p>
 * 요청 건의 세액공제/감면 항목별 입력 데이터를 저장한다.
 * 설계문서 section 12 기반.
 * </p>
 */
@Entity
@Table(name = "INP_DEDUCTION")
@IdClass(InpDeductionId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InpDeduction {

    /** 요청 ID (복합 PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 항목 카테고리 (복합 PK: INVEST, RD, EMPLOYMENT 등) */
    @Id
    @Column(name = "item_category", length = 30, nullable = false)
    private String itemCategory;

    /** 조세특례제한법 조항 (복합 PK) */
    @Id
    @Column(name = "provision", length = 30, nullable = false)
    private String provision;

    /** 귀속 연도 (복합 PK) */
    @Id
    @Column(name = "tax_year", length = 4, nullable = false)
    private String taxYear;

    /** 항목 순번 (복합 PK) */
    @Id
    @Column(name = "item_seq", nullable = false)
    private Integer itemSeq;

    /** 기준 금액 (원) */
    @Column(name = "base_amount")
    private Long baseAmount;

    /** 지역 구분 (수도권/비수도권 등) */
    @Column(name = "zone_type", length = 20)
    private String zoneType;

    /** 자산 유형 */
    @Column(name = "asset_type", length = 30)
    private String assetType;

    /** 연구개발 유형 (신성장/일반/위탁 등) */
    @Column(name = "rd_type", length = 30)
    private String rdType;

    /** 계산 방식 (증가분/당기분 등) */
    @Column(name = "method", length = 30)
    private String method;

    /** 세부 내역 (JSON 등 자유 형식) */
    @Lob
    @Column(name = "sub_detail")
    private String subDetail;

    /** 기적용 여부 */
    @Column(name = "existing_applied")
    private Boolean existingApplied;

    /** 기적용 금액 (원) */
    @Column(name = "existing_amount")
    private Long existingAmount;

    /** 이월공제 잔액 (원) */
    @Column(name = "carryforward_balance")
    private Long carryforwardBalance;

    @Builder
    public InpDeduction(String reqId, String itemCategory, String provision,
                        String taxYear, Integer itemSeq,
                        Long baseAmount, String zoneType, String assetType,
                        String rdType, String method, String subDetail,
                        Boolean existingApplied, Long existingAmount,
                        Long carryforwardBalance) {
        this.reqId = reqId;
        this.itemCategory = itemCategory;
        this.provision = provision;
        this.taxYear = taxYear;
        this.itemSeq = itemSeq;
        this.baseAmount = baseAmount;
        this.zoneType = zoneType;
        this.assetType = assetType;
        this.rdType = rdType;
        this.method = method;
        this.subDetail = subDetail;
        this.existingApplied = existingApplied;
        this.existingAmount = existingAmount;
        this.carryforwardBalance = carryforwardBalance;
    }
}
