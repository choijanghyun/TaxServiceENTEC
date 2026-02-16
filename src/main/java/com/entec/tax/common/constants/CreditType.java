package com.entec.tax.common.constants;

/**
 * 감면/공제 유형 코드
 */
public enum CreditType {

    EXEMPTION("EXEMPTION", "감면"),
    CREDIT("CREDIT", "공제");

    private final String code;
    private final String description;

    CreditType(String code, String description) {
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
     * 코드 문자열로부터 CreditType 을 반환한다.
     *
     * @param code 코드 문자열 (예: "EXEMPTION", "CREDIT")
     * @return 매칭되는 CreditType
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static CreditType fromCode(String code) {
        for (CreditType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CreditType code: " + code);
    }
}
