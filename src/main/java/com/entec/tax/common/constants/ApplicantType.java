package com.entec.tax.common.constants;

/**
 * 신청자 유형 코드
 */
public enum ApplicantType {

    CORP('C', "법인"),
    PERSONAL('P', "개인");

    private final char code;
    private final String description;

    ApplicantType(char code, String description) {
        this.code = code;
        this.description = description;
    }

    public char getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드 문자로부터 ApplicantType 을 반환한다.
     *
     * @param code 코드 문자 (예: 'C', 'P')
     * @return 매칭되는 ApplicantType
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static ApplicantType fromCode(char code) {
        for (ApplicantType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ApplicantType code: " + code);
    }
}
