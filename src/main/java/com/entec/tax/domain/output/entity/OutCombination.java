package com.entec.tax.domain.output.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * OUT_COMBINATION 테이블 엔티티 (§20).
 * <p>
 * 세액공제 조합 정보를 관리한다.
 * </p>
 */
@Entity
@Table(name = "OUT_COMBINATION")
@IdClass(OutCombinationId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutCombination {

    /** 요청 ID (PK) */
    @Id
    @Column(name = "req_id", length = 30, nullable = false)
    private String reqId;

    /** 조합 ID (PK) */
    @Id
    @Column(name = "combo_id", length = 30, nullable = false)
    private String comboId;

    /** 조합 순위 */
    @Column(name = "combo_rank")
    private Integer comboRank;

    /** 그룹 유형 */
    @Column(name = "group_type", length = 30)
    private String groupType;

    /** 조합명 */
    @Column(name = "combo_name", length = 200)
    private String comboName;

    /** 항목 JSON */
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    /** 면제 합계 */
    @Column(name = "exemption_total")
    private Long exemptionTotal;

    /** 공제 합계 */
    @Column(name = "credit_total")
    private Long creditTotal;

    /** 최저한세 조정액 */
    @Column(name = "min_tax_adj")
    private Long minTaxAdj;

    /** 농어촌특별세 합계 */
    @Column(name = "nongteuk_total")
    private Long nongteukTotal;

    /** 순 환급액 */
    @Column(name = "net_refund")
    private Long netRefund;

    /** 유효 여부 */
    @Column(name = "is_valid")
    private Boolean isValid;

    /** 적용 순서 */
    @Column(name = "application_order", columnDefinition = "TEXT")
    private String applicationOrder;

    /** 이월공제 항목 */
    @Column(name = "carryforward_items", columnDefinition = "TEXT")
    private String carryforwardItems;

    @Builder
    public OutCombination(String reqId, String comboId, Integer comboRank,
                           String groupType, String comboName, String itemsJson,
                           Long exemptionTotal, Long creditTotal, Long minTaxAdj,
                           Long nongteukTotal, Long netRefund, Boolean isValid,
                           String applicationOrder, String carryforwardItems) {
        this.reqId = reqId;
        this.comboId = comboId;
        this.comboRank = comboRank;
        this.groupType = groupType;
        this.comboName = comboName;
        this.itemsJson = itemsJson;
        this.exemptionTotal = exemptionTotal;
        this.creditTotal = creditTotal;
        this.minTaxAdj = minTaxAdj;
        this.nongteukTotal = nongteukTotal;
        this.netRefund = netRefund;
        this.isValid = isValid;
        this.applicationOrder = applicationOrder;
        this.carryforwardItems = carryforwardItems;
    }
}
