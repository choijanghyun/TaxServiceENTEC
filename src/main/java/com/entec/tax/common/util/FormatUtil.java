package com.entec.tax.common.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 포맷팅 유틸리티 클래스.
 *
 * <p>세무 서비스에서 사용하는 다양한 데이터의 포맷팅 기능을 제공합니다.</p>
 * <ul>
 *   <li>금액 포맷팅 (천 단위 콤마, 한글 변환)</li>
 *   <li>사업자번호 포맷팅 및 마스킹</li>
 *   <li>비율 포맷팅</li>
 *   <li>요청 ID(req_id) 생성</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class FormatUtil {

    /** 금액 포맷터 (천 단위 콤마) */
    private static final DecimalFormat AMOUNT_FORMATTER = new DecimalFormat("#,###");

    /** 날짜 포맷터 (YYYYMMDD) */
    private static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 한글 숫자 단위 배열 (일, 십, 백, 천) */
    private static final String[] KOREAN_DIGIT_UNITS = {"", "십", "백", "천"};

    /** 한글 큰 단위 배열 (만, 억, 조, 경) */
    private static final String[] KOREAN_LARGE_UNITS = {"", "만", "억", "조", "경"};

    /** 한글 숫자 배열 */
    private static final String[] KOREAN_NUMBERS = {
            "", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"
    };

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private FormatUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 금액을 천 단위 콤마가 포함된 문자열로 포맷팅합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>1234567 → "1,234,567원"</li>
     *   <li>0 → "0원"</li>
     *   <li>-1234567 → "-1,234,567원"</li>
     * </ul>
     *
     * @param amount 포맷팅할 금액 (단위: 원)
     * @return 천 단위 콤마가 포함된 금액 문자열 (예: "1,234,567원")
     */
    public static String formatAmount(long amount) {
        synchronized (AMOUNT_FORMATTER) {
            return AMOUNT_FORMATTER.format(amount) + "원";
        }
    }

    /**
     * 금액을 한글 표기로 변환합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>1234567 → "일백이십삼만사천오백육십칠원"</li>
     *   <li>10000 → "일만원"</li>
     *   <li>0 → "영원"</li>
     * </ul>
     *
     * @param amount 한글로 변환할 금액 (단위: 원, 양수만 지원)
     * @return 한글 금액 문자열 (예: "일백이십삼만사천오백육십칠원")
     * @throws IllegalArgumentException amount가 음수인 경우
     */
    public static String formatAmountKorean(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("한글 금액 변환은 음수를 지원하지 않습니다: " + amount);
        }
        if (amount == 0) {
            return "영원";
        }

        StringBuilder result = new StringBuilder();
        String numStr = String.valueOf(amount);
        int length = numStr.length();

        // 4자리씩 끊어서 처리 (경, 조, 억, 만, 일)
        int groupCount = (length + 3) / 4;

        for (int g = 0; g < groupCount; g++) {
            int startIdx = length - (g + 1) * 4;
            int endIdx = length - g * 4;
            if (startIdx < 0) {
                startIdx = 0;
            }

            String group = numStr.substring(startIdx, endIdx);
            String groupKorean = convertGroupToKorean(group);

            if (!groupKorean.isEmpty()) {
                result.insert(0, groupKorean + KOREAN_LARGE_UNITS[g]);
            }
        }

        result.append("원");
        return result.toString();
    }

    /**
     * 4자리 이하 숫자 그룹을 한글로 변환하는 내부 메서드.
     *
     * @param group 4자리 이하 숫자 문자열
     * @return 한글 변환 문자열
     */
    private static String convertGroupToKorean(String group) {
        StringBuilder sb = new StringBuilder();
        int len = group.length();

        for (int i = 0; i < len; i++) {
            int digit = group.charAt(i) - '0';
            int unitIndex = len - 1 - i;

            if (digit == 0) {
                continue;
            }

            // 1인 경우 "일"은 단위가 있을 때 생략하지 않음 (설계서 기준)
            sb.append(KOREAN_NUMBERS[digit]);
            sb.append(KOREAN_DIGIT_UNITS[unitIndex]);
        }

        return sb.toString();
    }

    /**
     * 사업자등록번호를 포맷팅합니다. (하이픈 구분)
     *
     * <p>10자리 숫자를 3-2-5 형식으로 포맷팅합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"1234567890" → "123-45-67890"</li>
     * </ul>
     *
     * @param raw 하이픈 없는 사업자등록번호 (10자리 숫자)
     * @return 포맷팅된 사업자등록번호 ("123-45-67890" 형식)
     * @throws IllegalArgumentException raw가 null이거나 10자리 숫자가 아닌 경우
     */
    public static String formatBizRegNo(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("사업자등록번호는 null이거나 빈 값일 수 없습니다.");
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            throw new IllegalArgumentException(
                    "사업자등록번호는 10자리 숫자여야 합니다. 입력값: " + raw + " (숫자 " + digits.length() + "자리)");
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
    }

    /**
     * 사업자등록번호를 마스킹합니다.
     *
     * <p>중간 5자리 중 뒤 3자리를 마스킹합니다. (개인정보 보호)</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"123-45-67890" → "123-45-***90"</li>
     * </ul>
     *
     * @param formatted 포맷팅된 사업자등록번호 ("123-45-67890" 형식)
     * @return 마스킹된 사업자등록번호 ("123-45-***90" 형식)
     * @throws IllegalArgumentException formatted가 null이거나 올바른 형식이 아닌 경우
     */
    public static String maskBizRegNo(String formatted) {
        if (formatted == null || formatted.trim().isEmpty()) {
            throw new IllegalArgumentException("사업자등록번호는 null이거나 빈 값일 수 없습니다.");
        }
        String digits = formatted.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            throw new IllegalArgumentException(
                    "사업자등록번호 형식이 올바르지 않습니다. 입력값: " + formatted);
        }
        // 123-45-***90 형식: 뒤 5자리 중 앞 3자리를 마스킹
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-"
                + "***" + digits.substring(8);
    }

    /**
     * 비율을 퍼센트 형식의 문자열로 포맷팅합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>BigDecimal("12.34") → "12.34%"</li>
     *   <li>BigDecimal("0.5") → "0.5%"</li>
     *   <li>BigDecimal("100") → "100%"</li>
     * </ul>
     *
     * @param rate 포맷팅할 비율 (BigDecimal)
     * @return 퍼센트 형식의 비율 문자열 (예: "12.34%")
     * @throws IllegalArgumentException rate가 null인 경우
     */
    public static String formatRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("비율은 null일 수 없습니다.");
        }
        return rate.stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * 요청 ID(req_id)를 생성합니다.
     *
     * <p>형식: {type}-{bizno(하이픈 제거)}-{YYYYMMDD}-{seq:03d}</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>applicantType="CORP", bizRegNo="123-45-67890", date=2024-01-15, seqNo=1
     *       → "CORP-1234567890-20240115-001"</li>
     *   <li>applicantType="IND", bizRegNo="9876543210", date=2024-03-20, seqNo=42
     *       → "IND-9876543210-20240320-042"</li>
     * </ul>
     *
     * @param applicantType 신청자 유형 (예: "CORP", "IND")
     * @param bizRegNo      사업자등록번호 (하이픈 포함/미포함 모두 가능, 10자리)
     * @param date          요청일자 (LocalDate)
     * @param seqNo         일련번호 (1 이상의 정수, 3자리 zero-padding)
     * @return 생성된 요청 ID 문자열
     * @throws IllegalArgumentException 필수 파라미터가 null이거나 유효하지 않은 경우
     */
    public static String formatReqId(String applicantType, String bizRegNo, LocalDate date, int seqNo) {
        if (applicantType == null || applicantType.trim().isEmpty()) {
            throw new IllegalArgumentException("신청자 유형은 null이거나 빈 값일 수 없습니다.");
        }
        if (bizRegNo == null || bizRegNo.trim().isEmpty()) {
            throw new IllegalArgumentException("사업자등록번호는 null이거나 빈 값일 수 없습니다.");
        }
        if (date == null) {
            throw new IllegalArgumentException("요청일자는 null일 수 없습니다.");
        }
        if (seqNo < 0) {
            throw new IllegalArgumentException("일련번호는 0 이상이어야 합니다. 입력값: " + seqNo);
        }

        String bizNoDigits = bizRegNo.replaceAll("[^0-9]", "");
        String dateStr = date.format(COMPACT_DATE_FORMATTER);
        String seqStr = String.format("%03d", seqNo);

        return applicantType.trim() + "-" + bizNoDigits + "-" + dateStr + "-" + seqStr;
    }
}
