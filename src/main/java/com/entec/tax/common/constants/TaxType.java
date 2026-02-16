package com.entec.tax.common.constants;

/**
 * 세금 유형 코드
 */
public enum TaxType {

    CORP("CORP", "법인세"),
    INC("INC", "종합소득세");

    private final String code;
    private final String description;

    TaxType(String code, String description) {
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
     * 코드 문자열로부터 TaxType 을 반환한다.
     *
     * @param code 코드 문자열 (예: "CORP", "INC")
     * @return 매칭되는 TaxType
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static TaxType fromCode(String code) {
        for (TaxType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TaxType code: " + code);
    }
}
