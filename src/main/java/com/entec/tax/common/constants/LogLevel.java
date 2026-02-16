package com.entec.tax.common.constants;

/**
 * 로그 레벨 코드
 */
public enum LogLevel {

    INFO("INFO", "정보"),
    WARN("WARN", "경고"),
    ERROR("ERROR", "오류"),
    DEBUG("DEBUG", "디버그");

    private final String code;
    private final String description;

    LogLevel(String code, String description) {
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
     * 코드 문자열로부터 LogLevel 을 반환한다.
     *
     * @param code 코드 문자열 (예: "INFO", "WARN")
     * @return 매칭되는 LogLevel
     * @throws IllegalArgumentException 매칭되는 코드가 없을 경우
     */
    public static LogLevel fromCode(String code) {
        for (LogLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown LogLevel code: " + code);
    }
}
