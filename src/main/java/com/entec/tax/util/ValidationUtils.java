package com.entec.tax.util;

import java.util.regex.Pattern;

/**
 * 유효성 검증 유틸리티 클래스
 * - 정규식 기반 각종 입력값 검증
 * - 사업자등록번호, 법인등록번호 등 세무 관련 검증 포함
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // === 정규식 패턴 ===
    /** 이메일 패턴 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 휴대전화 패턴 (010-1234-5678 또는 01012345678) */
    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");

    /** 일반 전화번호 패턴 (02-1234-5678 또는 031-123-4567) */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(0[2-6][0-5]?)-?(\\d{3,4})-?(\\d{4})$");

    /** 사업자등록번호 패턴 (123-45-67890 또는 1234567890) */
    private static final Pattern BIZ_NO_PATTERN =
            Pattern.compile("^\\d{3}-?\\d{2}-?\\d{5}$");

    /** 법인등록번호 패턴 (110111-1234567 또는 1101111234567) */
    private static final Pattern CORP_NO_PATTERN =
            Pattern.compile("^\\d{6}-?\\d{7}$");

    /** 숫자만 */
    private static final Pattern DIGITS_ONLY_PATTERN =
            Pattern.compile("^\\d+$");

    /** 한글만 */
    private static final Pattern KOREAN_ONLY_PATTERN =
            Pattern.compile("^[가-힣]+$");

    /** 영문만 */
    private static final Pattern ALPHA_ONLY_PATTERN =
            Pattern.compile("^[a-zA-Z]+$");

    /** 영문+숫자 */
    private static final Pattern ALPHANUMERIC_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]+$");

    /** 날짜 패턴 (yyyy-MM-dd) */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");

    /** 금액 패턴 (음수 허용, 콤마 허용) */
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("^-?[\\d,]+$");

    // === 이메일 검증 ===

    /**
     * 이메일 형식 검증
     */
    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // === 전화번호 검증 ===

    /**
     * 휴대전화번호 형식 검증
     */
    public static boolean isValidMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) {
            return false;
        }
        return MOBILE_PATTERN.matcher(mobile.trim()).matches();
    }

    /**
     * 전화번호 형식 검증 (일반전화 + 휴대전화)
     */
    public static boolean isValidPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return false;
        }
        String trimmed = phone.trim();
        return PHONE_PATTERN.matcher(trimmed).matches() || MOBILE_PATTERN.matcher(trimmed).matches();
    }

    // === 사업자등록번호 검증 ===

    /**
     * 사업자등록번호 형식 검증
     */
    public static boolean isValidBizNo(String bizNo) {
        if (StringUtils.isBlank(bizNo)) {
            return false;
        }
        if (!BIZ_NO_PATTERN.matcher(bizNo.trim()).matches()) {
            return false;
        }
        return checkBizNoChecksum(bizNo.trim());
    }

    /**
     * 사업자등록번호 체크섬 검증 (국세청 검증 로직)
     */
    private static boolean checkBizNoChecksum(String bizNo) {
        String digits = bizNo.replaceAll("-", "");
        if (digits.length() != 10) {
            return false;
        }

        int[] multipliers = {1, 3, 7, 1, 3, 7, 1, 3, 5};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * multipliers[i];
        }
        sum += ((digits.charAt(8) - '0') * 5) / 10;
        int checkDigit = (10 - (sum % 10)) % 10;

        return checkDigit == (digits.charAt(9) - '0');
    }

    // === 법인등록번호 검증 ===

    /**
     * 법인등록번호 형식 검증
     */
    public static boolean isValidCorpNo(String corpNo) {
        if (StringUtils.isBlank(corpNo)) {
            return false;
        }
        if (!CORP_NO_PATTERN.matcher(corpNo.trim()).matches()) {
            return false;
        }
        return checkCorpNoChecksum(corpNo.trim());
    }

    /**
     * 법인등록번호 체크섬 검증
     */
    private static boolean checkCorpNoChecksum(String corpNo) {
        String digits = corpNo.replaceAll("-", "");
        if (digits.length() != 13) {
            return false;
        }

        int[] multipliers = {1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * multipliers[i];
        }
        int checkDigit = (10 - (sum % 10)) % 10;

        return checkDigit == (digits.charAt(12) - '0');
    }

    // === 기본 타입 검증 ===

    /**
     * 숫자만 포함하는지 검증
     */
    public static boolean isDigitsOnly(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return DIGITS_ONLY_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * 한글만 포함하는지 검증
     */
    public static boolean isKoreanOnly(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return KOREAN_ONLY_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * 영문만 포함하는지 검증
     */
    public static boolean isAlphaOnly(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return ALPHA_ONLY_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * 영문+숫자만 포함하는지 검증
     */
    public static boolean isAlphanumeric(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(str.trim()).matches();
    }

    /**
     * 날짜 형식(yyyy-MM-dd) 검증
     */
    public static boolean isValidDateFormat(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return false;
        }
        return DATE_PATTERN.matcher(dateStr.trim()).matches();
    }

    /**
     * 금액 형식 검증 (숫자, 콤마, 음수 부호 허용)
     */
    public static boolean isValidAmount(String amount) {
        if (StringUtils.isBlank(amount)) {
            return false;
        }
        return AMOUNT_PATTERN.matcher(amount.trim()).matches();
    }

    // === 범위 검증 ===

    /**
     * 문자열 길이 범위 검증
     */
    public static boolean isLengthInRange(String str, int min, int max) {
        if (str == null) {
            return min == 0;
        }
        int len = str.length();
        return len >= min && len <= max;
    }

    /**
     * 숫자 범위 검증
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * 필수 입력값 존재 여부 검증
     */
    public static boolean isRequired(String str) {
        return StringUtils.isNotBlank(str);
    }
}
