package com.entec.tax.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 숫자 포맷팅 유틸리티 클래스
 * - 금액 포맷팅, 세액 계산, 숫자 변환 등
 * - 법인세 관련 숫자 처리에 특화
 */
public final class NumberFormatUtils {

    private NumberFormatUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,###");
    private static final DecimalFormat AMOUNT_FORMAT_WITH_SIGN = new DecimalFormat("+#,###;-#,###");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("#,##0.##");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#,##0.##%");

    /**
     * 금액 포맷팅 (3자리 콤마)
     * (예: 1234567 -> "1,234,567")
     */
    public static String formatAmount(long amount) {
        synchronized (AMOUNT_FORMAT) {
            return AMOUNT_FORMAT.format(amount);
        }
    }

    /**
     * 금액 포맷팅 (BigDecimal)
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        synchronized (AMOUNT_FORMAT) {
            return AMOUNT_FORMAT.format(amount);
        }
    }

    /**
     * 금액 포맷팅 (원 단위 포함)
     * (예: 1234567 -> "1,234,567원")
     */
    public static String formatAmountWon(long amount) {
        return formatAmount(amount) + "원";
    }

    /**
     * 부호 포함 금액 포맷팅
     * (예: 1234567 -> "+1,234,567", -1234567 -> "-1,234,567")
     */
    public static String formatAmountWithSign(long amount) {
        if (amount == 0) {
            return "0";
        }
        synchronized (AMOUNT_FORMAT_WITH_SIGN) {
            return AMOUNT_FORMAT_WITH_SIGN.format(amount);
        }
    }

    /**
     * 비율 포맷팅
     * (예: 0.09 -> "9", 0.19 -> "19")
     */
    public static String formatRate(double rate) {
        synchronized (RATE_FORMAT) {
            return RATE_FORMAT.format(rate * 100);
        }
    }

    /**
     * 퍼센트 포맷팅
     * (예: 0.09 -> "9%")
     */
    public static String formatPercent(double rate) {
        synchronized (PERCENT_FORMAT) {
            return PERCENT_FORMAT.format(rate);
        }
    }

    /**
     * 문자열을 Long으로 변환 (콤마 제거)
     * @param str 숫자 문자열 (콤마 포함 가능)
     * @return Long 값, 변환 실패 시 0
     */
    public static long parseLong(String str) {
        return parseLong(str, 0L);
    }

    /**
     * 문자열을 Long으로 변환 (콤마 제거, 기본값 지정)
     */
    public static long parseLong(String str, long defaultValue) {
        if (StringUtils.isBlank(str)) {
            return defaultValue;
        }
        try {
            String cleaned = str.trim().replaceAll("[,\\s]", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 문자열을 BigDecimal로 변환 (콤마 제거)
     */
    public static BigDecimal parseBigDecimal(String str) {
        if (StringUtils.isBlank(str)) {
            return BigDecimal.ZERO;
        }
        try {
            String cleaned = str.trim().replaceAll("[,\\s]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 원 단위 절사 (10원 미만 절사)
     * (예: 12345 -> 12340)
     */
    public static long truncateToTens(long amount) {
        return (amount / 10) * 10;
    }

    /**
     * 원 단위 절사 (100원 미만 절사)
     */
    public static long truncateToHundreds(long amount) {
        return (amount / 100) * 100;
    }

    /**
     * 원 단위 절사 (1,000원 미만 절사)
     * - 법인세 계산 시 자주 사용
     */
    public static long truncateToThousands(long amount) {
        return (amount / 1000) * 1000;
    }

    /**
     * 반올림 (지정 자릿수)
     */
    public static BigDecimal round(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 절사 (지정 자릿수)
     */
    public static BigDecimal truncate(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, RoundingMode.DOWN);
    }

    /**
     * null-safe 덧셈
     */
    public static long add(Long a, Long b) {
        long valA = (a != null) ? a : 0L;
        long valB = (b != null) ? b : 0L;
        return valA + valB;
    }

    /**
     * null-safe 뺄셈
     */
    public static long subtract(Long a, Long b) {
        long valA = (a != null) ? a : 0L;
        long valB = (b != null) ? b : 0L;
        return valA - valB;
    }

    /**
     * 양수 여부 확인
     */
    public static boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    /**
     * 음수 여부 확인
     */
    public static boolean isNegative(Long value) {
        return value != null && value < 0;
    }

    /**
     * 0 여부 확인
     */
    public static boolean isZero(Long value) {
        return value == null || value == 0;
    }

    /**
     * 숫자를 한글 금액으로 변환
     * (예: 123 -> "일이삼")
     */
    public static String toKoreanNumber(long number) {
        if (number == 0) {
            return "영";
        }

        String[] koreanDigits = {"", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"};
        String[] units = {"", "십", "백", "천"};
        String[] bigUnits = {"", "만", "억", "조", "경"};

        boolean isNegative = number < 0;
        String numStr = String.valueOf(Math.abs(number));

        StringBuilder result = new StringBuilder();
        int len = numStr.length();

        for (int i = 0; i < len; i++) {
            int digit = numStr.charAt(i) - '0';
            int position = len - 1 - i;
            int unitIndex = position % 4;
            int bigUnitIndex = position / 4;

            if (digit != 0) {
                if (digit == 1 && unitIndex > 0) {
                    result.append(units[unitIndex]);
                } else {
                    result.append(koreanDigits[digit]).append(units[unitIndex]);
                }
            }

            if (unitIndex == 0 && result.length() > 0) {
                // 만, 억, 조 단위 붙이기
                boolean hasDigitsInGroup = false;
                for (int j = Math.max(0, i - 3); j <= i; j++) {
                    if (numStr.charAt(j) != '0') {
                        hasDigitsInGroup = true;
                        break;
                    }
                }
                if (hasDigitsInGroup && bigUnitIndex < bigUnits.length) {
                    result.append(bigUnits[bigUnitIndex]);
                }
            }
        }

        if (isNegative) {
            return "마이너스 " + result.toString();
        }
        return result.toString();
    }
}
