package com.entec.tax.common.constants;

/**
 * INP_DEDUCTION 항목 카테고리 코드
 */
public enum ItemCategory {

    INVEST("INVEST", "투자세액공제"),
    RD("RD", "연구개발세액공제"),
    STARTUP("STARTUP", "창업중소기업감면"),
    SME_SPECIAL("SME_SPECIAL", "중소기업특별세액감면"),
    EMPLOYMENT("EMPLOYMENT", "통합고용세액공제"),
    SOCIAL_INS("SOCIAL_INS", "사회보험료세액공제"),
    FOREIGN_TAX("FOREIGN_TAX", "외국납부세액공제"),
    INC_SINCERITY("INC_SINCERITY", "성실사업자세액공제"),
    INC_LANDLORD("INC_LANDLORD", "착한임대인세액공제"),
    INC_SINCERITY_CONFIRM("INC_SINCERITY_CONFIRM", "성실신고확인비용세액공제");

    private final String code;
    private final String description;

    ItemCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드 문자열로부터 ItemCategory 를 반환한다.
     *
     * @param code 코드 문자열 (예: "INVEST", "RD")
     * @return 매칭되는 ItemCategory
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static ItemCategory fromCode(String code) {
        for (ItemCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown ItemCategory code: " + code);
    }
}
