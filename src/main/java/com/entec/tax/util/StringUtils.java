package com.entec.tax.util;

/**
 * 문자열 조작 유틸리티 클래스
 * - 문자열 관련 범용 기능 제공
 */
public final class StringUtils {

    private StringUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    /**
     * null 또는 빈 문자열 여부 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * null, 빈 문자열, 공백만 있는 문자열 여부 확인
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * null이 아니고 빈 문자열이 아닌지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * null이 아니고 공백이 아닌 문자가 포함되어 있는지 확인
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * null을 빈 문자열로 치환
     */
    public static String nvl(String str) {
        return str == null ? "" : str;
    }

    /**
     * null일 경우 기본값 반환
     */
    public static String nvl(String str, String defaultValue) {
        return str == null ? defaultValue : str;
    }

    /**
     * 사업자등록번호 포맷팅 (123-45-67890)
     * @param bizNo 숫자만 포함된 사업자등록번호 (10자리)
     * @return 포맷팅된 사업자등록번호
     */
    public static String formatBizNo(String bizNo) {
        if (isEmpty(bizNo)) {
            return "";
        }
        String digits = bizNo.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return bizNo;
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
    }

    /**
     * 법인등록번호 포맷팅 (110111-1234567)
     * @param corpNo 숫자만 포함된 법인등록번호 (13자리)
     * @return 포맷팅된 법인등록번호
     */
    public static String formatCorpNo(String corpNo) {
        if (isEmpty(corpNo)) {
            return "";
        }
        String digits = corpNo.replaceAll("[^0-9]", "");
        if (digits.length() != 13) {
            return corpNo;
        }
        return digits.substring(0, 6) + "-" + digits.substring(6);
    }

    /**
     * 전화번호 포맷팅 (02-1234-5678, 010-1234-5678)
     * @param phoneNo 숫자만 포함된 전화번호
     * @return 포맷팅된 전화번호
     */
    public static String formatPhoneNo(String phoneNo) {
        if (isEmpty(phoneNo)) {
            return "";
        }
        String digits = phoneNo.replaceAll("[^0-9]", "");

        if (digits.length() == 11) {
            // 010-1234-5678
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        } else if (digits.length() == 10) {
            if (digits.startsWith("02")) {
                // 02-1234-5678
                return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
            } else {
                // 031-123-4567
                return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
            }
        } else if (digits.length() == 9 && digits.startsWith("02")) {
            // 02-123-4567
            return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
        }
        return phoneNo;
    }

    /**
     * 문자열에서 숫자만 추출
     */
    public static String extractDigits(String str) {
        if (isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[^0-9]", "");
    }

    /**
     * 문자열 좌측 패딩
     * @param str 원본 문자열
     * @param length 전체 길이
     * @param padChar 패딩 문자
     * @return 패딩된 문자열
     */
    public static String leftPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length - str.length(); i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * 문자열 우측 패딩
     */
    public static String rightPad(String str, int length, char padChar) {
        if (str == null) {
            str = "";
        }
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        for (int i = 0; i < length - str.length(); i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    /**
     * 문자열 마스킹 (중간 부분 *)
     * @param str 원본 문자열
     * @param frontLen 앞에서 보여줄 길이
     * @param backLen 뒤에서 보여줄 길이
     * @return 마스킹된 문자열
     */
    public static String mask(String str, int frontLen, int backLen) {
        if (isEmpty(str)) {
            return "";
        }
        if (str.length() <= frontLen + backLen) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, frontLen));
        for (int i = 0; i < str.length() - frontLen - backLen; i++) {
            sb.append('*');
        }
        sb.append(str.substring(str.length() - backLen));
        return sb.toString();
    }

    /**
     * 사업자등록번호 마스킹 (123-45-***90)
     */
    public static String maskBizNo(String bizNo) {
        String formatted = formatBizNo(bizNo);
        if (formatted.length() < 12) {
            return formatted;
        }
        return formatted.substring(0, 7) + "***" + formatted.substring(10);
    }

    /**
     * 카멜 케이스를 스네이크 케이스로 변환
     * (예: "userName" -> "user_name")
     */
    public static String camelToSnake(String camelCase) {
        if (isEmpty(camelCase)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 스네이크 케이스를 카멜 케이스로 변환
     * (예: "user_name" -> "userName")
     */
    public static String snakeToCamel(String snakeCase) {
        if (isEmpty(snakeCase)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 문자열 길이 바이트 단위 계산 (EUC-KR 기준, 한글 2바이트)
     */
    public static int getByteLength(String str) {
        if (isEmpty(str)) {
            return 0;
        }
        int byteLen = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c > 0x7F) {
                byteLen += 2;
            } else {
                byteLen += 1;
            }
        }
        return byteLen;
    }

    /**
     * 문자열을 바이트 단위로 자르기 (한글 깨짐 방지)
     */
    public static String substringByByte(String str, int maxBytes) {
        if (isEmpty(str)) {
            return "";
        }
        int byteLen = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int charBytes = (c > 0x7F) ? 2 : 1;
            if (byteLen + charBytes > maxBytes) {
                break;
            }
            byteLen += charBytes;
            sb.append(c);
        }
        return sb.toString();
    }
}
