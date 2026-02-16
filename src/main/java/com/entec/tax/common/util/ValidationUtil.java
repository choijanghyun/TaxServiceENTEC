package com.entec.tax.common.util;

import java.util.regex.Pattern;

/**
 * 유효성 검증 유틸리티 클래스.
 *
 * <p>세무 서비스에서 사용하는 다양한 데이터의 유효성을 검증합니다.</p>
 * <ul>
 *   <li>사업자등록번호 체크섬 검증</li>
 *   <li>법인등록번호 검증</li>
 *   <li>업종코드(KSIC) 검증</li>
 *   <li>요청 ID(req_id) 형식 검증</li>
 *   <li>과세연도 검증</li>
 *   <li>기본 값 검증 (비어있는지, 양수인지, 음수가 아닌지)</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class ValidationUtil {

    /**
     * 사업자등록번호 체크섬 계산에 사용되는 가중치 배열.
     * <p>국세청 사업자등록번호 검증 알고리즘에 따른 가중치입니다.</p>
     */
    private static final int[] BIZ_REG_WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    /**
     * req_id 형식 검증 정규식.
     * <p>형식: {type}-{bizno(10자리숫자)}-{YYYYMMDD}-{seq:3자리숫자}</p>
     * <p>예: CORP-1234567890-20240115-001</p>
     */
    private static final Pattern REQ_ID_PATTERN =
            Pattern.compile("^[A-Z_]+-\\d{10}-\\d{8}-\\d{3}$");

    /**
     * 과세연도 검증 정규식 (4자리 숫자).
     */
    private static final Pattern TAX_YEAR_PATTERN = Pattern.compile("^\\d{4}$");

    /**
     * 업종코드(KSIC) 검증 정규식 (5자리 숫자 또는 영문+숫자).
     */
    private static final Pattern KSIC_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{5}$");

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private ValidationUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 사업자등록번호의 유효성을 체크섬으로 검증합니다.
     *
     * <p>국세청 사업자등록번호 검증 알고리즘을 사용합니다.</p>
     * <p>검증 절차:</p>
     * <ol>
     *   <li>10자리 숫자 여부 확인</li>
     *   <li>각 자릿수에 가중치(1,3,7,1,3,7,1,3,5)를 곱하여 합산</li>
     *   <li>9번째 자릿수 * 5 / 10 값을 추가 합산</li>
     *   <li>합산값을 10으로 나눈 나머지와 마지막 자릿수의 합이 10의 배수이면 유효</li>
     * </ol>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"1234567890" → 체크섬 검증 결과 (true/false)</li>
     *   <li>"123-45-67890" → 하이픈 제거 후 검증</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param bizRegNo 사업자등록번호 (하이픈 포함/미포함 모두 가능)
     * @return 유효한 사업자등록번호이면 true, 그렇지 않으면 false
     */
    public static boolean isValidBizRegNo(String bizRegNo) {
        if (bizRegNo == null || bizRegNo.trim().isEmpty()) {
            return false;
        }

        String digits = bizRegNo.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * BIZ_REG_WEIGHTS[i];
        }

        // 9번째 자릿수(인덱스 8)에 5를 곱한 값의 십의 자리를 추가
        sum += ((digits.charAt(8) - '0') * 5) / 10;

        int remainder = sum % 10;
        int lastDigit = digits.charAt(9) - '0';

        return (remainder + lastDigit) % 10 == 0;
    }

    /**
     * 법인등록번호의 유효성을 검증합니다.
     *
     * <p>법인등록번호는 13자리 숫자이며, 체크디짓 검증을 수행합니다.</p>
     * <p>검증 절차:</p>
     * <ol>
     *   <li>13자리 숫자 여부 확인</li>
     *   <li>각 자릿수에 가중치(1,2,1,2,1,2,1,2,1,2,1,2)를 곱하여 합산</li>
     *   <li>합산값을 10으로 나눈 나머지를 10에서 빼고, 다시 10으로 나눈 나머지가 마지막 자릿수와 일치하면 유효</li>
     * </ol>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"1101111234567" → 체크섬 검증 결과 (true/false)</li>
     *   <li>"110111-1234567" → 하이픈 제거 후 검증</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param corpRegNo 법인등록번호 (하이픈 포함/미포함 모두 가능)
     * @return 유효한 법인등록번호이면 true, 그렇지 않으면 false
     */
    public static boolean isValidCorpRegNo(String corpRegNo) {
        if (corpRegNo == null || corpRegNo.trim().isEmpty()) {
            return false;
        }

        String digits = corpRegNo.replaceAll("[^0-9]", "");
        if (digits.length() != 13) {
            return false;
        }

        // 가중치: 1,2 반복 (12자리)
        int[] weights = {1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        int lastDigit = digits.charAt(12) - '0';

        return checkDigit == lastDigit;
    }

    /**
     * 업종코드(KSIC)의 유효성을 검증합니다.
     *
     * <p>한국표준산업분류(KSIC) 코드는 5자리 영문/숫자로 구성됩니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"47190" → true (5자리 숫자)</li>
     *   <li>"A0111" → true (5자리 영문+숫자)</li>
     *   <li>"1234" → false (4자리)</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param code 업종코드 (5자리 영문/숫자)
     * @return 유효한 업종코드이면 true, 그렇지 않으면 false
     */
    public static boolean isValidKsicCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return KSIC_CODE_PATTERN.matcher(code.trim()).matches();
    }

    /**
     * 요청 ID(req_id)의 형식을 검증합니다.
     *
     * <p>req_id 형식: {type}-{bizno(10자리숫자)}-{YYYYMMDD(8자리)}-{seq(3자리숫자)}</p>
     * <p>예: CORP-1234567890-20240115-001</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"CORP-1234567890-20240115-001" → true</li>
     *   <li>"IND-9876543210-20240320-042" → true</li>
     *   <li>"INVALID" → false</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param reqId 검증할 요청 ID 문자열
     * @return 유효한 req_id 형식이면 true, 그렇지 않으면 false
     */
    public static boolean isValidReqId(String reqId) {
        if (reqId == null || reqId.trim().isEmpty()) {
            return false;
        }
        return REQ_ID_PATTERN.matcher(reqId.trim()).matches();
    }

    /**
     * 과세연도의 유효성을 검증합니다.
     *
     * <p>과세연도는 4자리 숫자(YYYY)여야 합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"2023" → true</li>
     *   <li>"2024" → true</li>
     *   <li>"23" → false (2자리)</li>
     *   <li>"abcd" → false (숫자가 아님)</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param year 검증할 과세연도 문자열 (4자리 숫자)
     * @return 유효한 과세연도이면 true, 그렇지 않으면 false
     */
    public static boolean isValidTaxYear(String year) {
        if (year == null || year.trim().isEmpty()) {
            return false;
        }
        return TAX_YEAR_PATTERN.matcher(year.trim()).matches();
    }

    /**
     * 문자열이 비어있지 않은지 검증합니다.
     *
     * <p>null, 빈 문자열(""), 공백만 있는 문자열(" ")을 비어있는 것으로 판단합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"hello" → true</li>
     *   <li>"" → false</li>
     *   <li>" " → false</li>
     *   <li>null → false</li>
     * </ul>
     *
     * @param str 검증할 문자열
     * @return 비어있지 않으면 true, 비어있으면 false
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * 값이 양수(0보다 큰 값)인지 검증합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>1 → true</li>
     *   <li>100 → true</li>
     *   <li>0 → false</li>
     *   <li>-1 → false</li>
     * </ul>
     *
     * @param val 검증할 값
     * @return 양수이면 true, 0 또는 음수이면 false
     */
    public static boolean isPositive(long val) {
        return val > 0;
    }

    /**
     * 값이 음수가 아닌지(0 이상인지) 검증합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>0 → true</li>
     *   <li>1 → true</li>
     *   <li>100 → true</li>
     *   <li>-1 → false</li>
     * </ul>
     *
     * @param val 검증할 값
     * @return 0 이상이면 true, 음수이면 false
     */
    public static boolean isNonNegative(long val) {
        return val >= 0;
    }
}
