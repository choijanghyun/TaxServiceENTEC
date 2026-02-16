package com.entec.tax.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 금액 절사 유틸리티 클래스.
 *
 * <p>설계서 §1.3 절사원칙에 따라 모든 금액 및 비율 계산 시 절사(TRUNCATE)를 적용합니다.</p>
 * <ul>
 *   <li>금액: 10원 미만 절사</li>
 *   <li>비율: 소수점 이하 지정 자릿수에서 절사</li>
 *   <li>환급가산금: 1원 미만 절사</li>
 * </ul>
 *
 * <p><strong>중요: 반올림(ROUND)은 절대 사용하지 않습니다. 모든 연산에 RoundingMode.DOWN(절사)을 적용합니다.</strong></p>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class TruncationUtil {

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private TruncationUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 금액을 10원 미만 절사합니다.
     *
     * <p>설계서 §1.3 절사원칙 - 금액 10원 미만 절사.</p>
     * <p>계산식: (amount / 10) * 10</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>1,234,567 → 1,234,560</li>
     *   <li>999 → 990</li>
     *   <li>5 → 0</li>
     *   <li>-1,234,567 → -1,234,560 (음수의 경우도 0 방향으로 절사)</li>
     * </ul>
     *
     * @param amount 절사 대상 금액 (단위: 원)
     * @return 10원 미만이 절사된 금액 (단위: 원)
     */
    public static long truncateAmount(long amount) {
        return (amount / 10) * 10;
    }

    /**
     * 비율(세율 등)을 지정된 소수점 자릿수에서 절사합니다.
     *
     * <p>설계서 §1.3 절사원칙 - 비율 소수점 이하 지정 자릿수에서 절사.</p>
     * <p>RoundingMode.DOWN을 사용하여 절사합니다. 반올림(HALF_UP 등)은 절대 사용하지 않습니다.</p>
     *
     * <p>예시 (scale=3):</p>
     * <ul>
     *   <li>0.123456 → 0.123</li>
     *   <li>0.999999 → 0.999</li>
     *   <li>12.3456 → 12.345</li>
     * </ul>
     *
     * @param rate  절사 대상 비율 (BigDecimal). null인 경우 BigDecimal.ZERO를 반환합니다.
     * @param scale 소수점 이하 유지할 자릿수 (예: 3이면 소수점 셋째자리까지 유지)
     * @return 지정된 자릿수에서 절사된 비율 (BigDecimal)
     * @throws IllegalArgumentException scale이 음수인 경우
     */
    public static BigDecimal truncateRate(BigDecimal rate, int scale) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale은 0 이상이어야 합니다. 입력값: " + scale);
        }
        return rate.setScale(scale, RoundingMode.DOWN);
    }

    /**
     * 환급가산금을 1원 미만 절사합니다.
     *
     * <p>설계서 §1.3 절사원칙 - 환급가산금 1원 미만 절사.</p>
     * <p>long 타입은 이미 정수이므로 소수점 이하가 없습니다.
     * 이 메서드는 의미론적 명확성을 위해 제공되며, double 등의 계산 결과를 long으로 변환할 때 사용합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>1234 → 1234 (long 타입이므로 이미 1원 미만 절사됨)</li>
     * </ul>
     *
     * @param amount 환급가산금 (단위: 원, long 타입으로 이미 1원 미만 절사됨)
     * @return 1원 미만이 절사된 환급가산금 (단위: 원)
     */
    public static long truncateInterest(long amount) {
        return amount;
    }

    /**
     * BigDecimal 값을 지정된 소수점 자릿수에서 절사합니다.
     *
     * <p>설계서 §1.3 절사원칙에 따라 RoundingMode.DOWN(절사)을 적용합니다.</p>
     * <p>반올림(ROUND)은 절대 사용하지 않습니다.</p>
     *
     * <p>예시 (scale=2):</p>
     * <ul>
     *   <li>123.456 → 123.45</li>
     *   <li>999.999 → 999.99</li>
     *   <li>0.001 → 0.00</li>
     * </ul>
     *
     * <p>예시 (scale=0):</p>
     * <ul>
     *   <li>123.456 → 123</li>
     *   <li>999.999 → 999</li>
     * </ul>
     *
     * @param value 절사 대상 BigDecimal 값. null인 경우 BigDecimal.ZERO를 반환합니다.
     * @param scale 소수점 이하 유지할 자릿수
     * @return 지정된 자릿수에서 절사된 BigDecimal 값
     * @throws IllegalArgumentException scale이 음수인 경우
     */
    public static BigDecimal truncateBigDecimal(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (scale < 0) {
            throw new IllegalArgumentException("scale은 0 이상이어야 합니다. 입력값: " + scale);
        }
        return value.setScale(scale, RoundingMode.DOWN);
    }
}
