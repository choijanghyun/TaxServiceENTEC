package com.entec.tax.common.constants;

/**
 * 위험 등급 코드
 */
public enum RiskLevel {

    HIGH("HIGH", "상"),
    MEDIUM("MEDIUM", "중"),
    LOW("LOW", "하");

    private final String code;
    private final String description;

    RiskLevel(String code, String description) {
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
     * 코드 문자열로부터 RiskLevel 을 반환한다.
     *
     * @param code 코드 문자열 (예: "HIGH", "MEDIUM", "LOW")
     * @return 매칭되는 RiskLevel
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static RiskLevel fromCode(String code) {
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown RiskLevel code: " + code);
    }
}
