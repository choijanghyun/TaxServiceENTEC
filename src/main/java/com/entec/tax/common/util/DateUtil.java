package com.entec.tax.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 날짜/시간 유틸리티 클래스.
 *
 * <p>세무 서비스에서 사용하는 날짜 관련 연산을 제공합니다.</p>
 * <ul>
 *   <li>날짜 파싱 및 포맷팅 (ISO 8601, YYYYMMDD)</li>
 *   <li>법정신고기한 산출 (법인세, 소득세)</li>
 *   <li>경정청구기한 산출 (법정신고기한 + 5년)</li>
 *   <li>과세연도 경계 산출</li>
 *   <li>나이 계산, 병역기간 계산</li>
 * </ul>
 *
 * @author ENTEC Tax Service
 * @since 1.0.0
 */
public final class DateUtil {

    /** ISO 8601 날짜 포맷 (YYYY-MM-DD) */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 압축 날짜 포맷 (YYYYMMDD) */
    private static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 병역기간 최대 연수 */
    private static final long MAX_MILITARY_YEARS = 6L;

    /** 법인세 세목 코드 */
    private static final String TAX_TYPE_CORP = "CORP";

    /** 소득세 세목 코드 */
    private static final String TAX_TYPE_INC = "INC";

    /** 성실신고 세목 코드 */
    private static final String TAX_TYPE_INC_FAITHFUL = "INC_FAITHFUL";

    /**
     * 인스턴스 생성 방지를 위한 private 생성자.
     */
    private DateUtil() {
        throw new UnsupportedOperationException("유틸리티 클래스는 인스턴스화할 수 없습니다.");
    }

    /**
     * 문자열을 LocalDate로 파싱합니다.
     *
     * <p>ISO 8601 형식(YYYY-MM-DD) 또는 압축 형식(YYYYMMDD)을 지원합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"2024-01-15" → LocalDate(2024, 1, 15)</li>
     *   <li>"20240115" → LocalDate(2024, 1, 15)</li>
     * </ul>
     *
     * @param dateStr 파싱할 날짜 문자열 (ISO 8601: "YYYY-MM-DD" 또는 압축: "YYYYMMDD")
     * @return 파싱된 LocalDate 객체
     * @throws IllegalArgumentException dateStr이 null이거나 빈 문자열인 경우
     * @throws DateTimeParseException   날짜 형식이 올바르지 않은 경우
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜 문자열은 null이거나 빈 값일 수 없습니다.");
        }
        String trimmed = dateStr.trim();
        if (trimmed.contains("-")) {
            return LocalDate.parse(trimmed, ISO_DATE_FORMATTER);
        }
        return LocalDate.parse(trimmed, COMPACT_DATE_FORMATTER);
    }

    /**
     * LocalDate를 ISO 8601 형식 문자열로 변환합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>LocalDate(2024, 1, 15) → "2024-01-15"</li>
     * </ul>
     *
     * @param date 변환할 LocalDate 객체
     * @return ISO 8601 형식의 날짜 문자열 ("YYYY-MM-DD")
     * @throws IllegalArgumentException date가 null인 경우
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜 객체는 null일 수 없습니다.");
        }
        return date.format(ISO_DATE_FORMATTER);
    }

    /**
     * LocalDate를 압축 형식 문자열로 변환합니다.
     *
     * <p>예시:</p>
     * <ul>
     *   <li>LocalDate(2024, 1, 15) → "20240115"</li>
     * </ul>
     *
     * @param date 변환할 LocalDate 객체
     * @return 압축 형식의 날짜 문자열 ("YYYYMMDD")
     * @throws IllegalArgumentException date가 null인 경우
     */
    public static String formatDateCompact(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("날짜 객체는 null일 수 없습니다.");
        }
        return date.format(COMPACT_DATE_FORMATTER);
    }

    /**
     * 법정신고기한을 산출합니다.
     *
     * <p>세목별 법정신고기한:</p>
     * <ul>
     *   <li><strong>CORP (법인세)</strong>: 사업연도 종료일(과세연도 12.31) + 3개월 → 다음해 3.31</li>
     *   <li><strong>INC (소득세)</strong>: 과세연도 다음해 5.31</li>
     *   <li><strong>INC_FAITHFUL (성실신고 소득세)</strong>: 과세연도 다음해 6.30</li>
     * </ul>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>taxYear="2023", taxType="CORP" → 2024-03-31</li>
     *   <li>taxYear="2023", taxType="INC" → 2024-05-31</li>
     *   <li>taxYear="2023", taxType="INC_FAITHFUL" → 2024-06-30</li>
     * </ul>
     *
     * @param taxYear 과세연도 (4자리 연도 문자열, 예: "2023")
     * @param taxType 세목 코드 ("CORP": 법인세, "INC": 소득세, "INC_FAITHFUL": 성실신고 소득세)
     * @return 법정신고기한 (LocalDate)
     * @throws IllegalArgumentException taxYear 또는 taxType이 null이거나 유효하지 않은 경우
     */
    public static LocalDate getFilingDeadline(String taxYear, String taxType) {
        if (taxYear == null || taxYear.trim().isEmpty()) {
            throw new IllegalArgumentException("과세연도는 null이거나 빈 값일 수 없습니다.");
        }
        if (taxType == null || taxType.trim().isEmpty()) {
            throw new IllegalArgumentException("세목 코드는 null이거나 빈 값일 수 없습니다.");
        }

        int year = Integer.parseInt(taxYear.trim());
        String type = taxType.trim().toUpperCase();

        switch (type) {
            case TAX_TYPE_CORP:
                // 법인세: 사업연도종료(12.31) + 3개월 → 다음해 3.31
                return LocalDate.of(year + 1, 3, 31);

            case TAX_TYPE_INC:
                // 소득세: 다음해 5.31
                return LocalDate.of(year + 1, 5, 31);

            case TAX_TYPE_INC_FAITHFUL:
                // 성실신고 소득세: 다음해 6.30
                return LocalDate.of(year + 1, 6, 30);

            default:
                throw new IllegalArgumentException(
                        "지원하지 않는 세목 코드입니다: " + taxType
                                + " (지원 코드: CORP, INC, INC_FAITHFUL)");
        }
    }

    /**
     * 경정청구기한을 산출합니다.
     *
     * <p>경정청구기한 = 법정신고기한 + 5년</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>filingDeadline=2024-03-31 → 2029-03-31</li>
     *   <li>filingDeadline=2024-05-31 → 2029-05-31</li>
     * </ul>
     *
     * @param filingDeadline 법정신고기한 (LocalDate)
     * @return 경정청구기한 (법정신고기한으로부터 5년 후, LocalDate)
     * @throws IllegalArgumentException filingDeadline이 null인 경우
     */
    public static LocalDate getClaimDeadline(LocalDate filingDeadline) {
        if (filingDeadline == null) {
            throw new IllegalArgumentException("법정신고기한은 null일 수 없습니다.");
        }
        return filingDeadline.plusYears(5);
    }

    /**
     * 두 날짜 사이의 일수를 계산합니다.
     *
     * <p>start부터 end까지의 일수를 반환합니다 (end - start).
     * start가 end보다 이후인 경우 음수를 반환합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>start=2024-01-01, end=2024-01-31 → 30</li>
     *   <li>start=2024-01-01, end=2024-01-01 → 0</li>
     *   <li>start=2024-01-31, end=2024-01-01 → -30</li>
     * </ul>
     *
     * @param start 시작일 (LocalDate)
     * @param end   종료일 (LocalDate)
     * @return 시작일부터 종료일까지의 일수 (long)
     * @throws IllegalArgumentException start 또는 end가 null인 경우
     */
    public static long calculateDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("시작일과 종료일은 null일 수 없습니다.");
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 과세연도의 회계연도(fiscal year) 경계를 반환합니다.
     *
     * <p>과세연도 시작일(1.1)과 종료일(12.31)을 배열로 반환합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>"2023" → [LocalDate(2023, 1, 1), LocalDate(2023, 12, 31)]</li>
     * </ul>
     *
     * @param taxYear 과세연도 (4자리 연도 문자열, 예: "2023")
     * @return 회계연도 경계 배열 [시작일, 종료일] (LocalDate[2])
     * @throws IllegalArgumentException taxYear가 null이거나 빈 값인 경우
     * @throws NumberFormatException    taxYear가 숫자가 아닌 경우
     */
    public static LocalDate[] getFiscalYear(String taxYear) {
        if (taxYear == null || taxYear.trim().isEmpty()) {
            throw new IllegalArgumentException("과세연도는 null이거나 빈 값일 수 없습니다.");
        }
        int year = Integer.parseInt(taxYear.trim());
        return new LocalDate[]{
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        };
    }

    /**
     * 기준일 현재의 만 나이를 계산합니다.
     *
     * <p>한국 세법상 만 나이(counting from birth date to base date)를 계산합니다.</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>birthDate=1990-03-15, baseDate=2024-03-14 → 33세</li>
     *   <li>birthDate=1990-03-15, baseDate=2024-03-15 → 34세</li>
     * </ul>
     *
     * @param birthDate 생년월일 (LocalDate)
     * @param baseDate  기준일 (LocalDate)
     * @return 만 나이 (int)
     * @throws IllegalArgumentException birthDate 또는 baseDate가 null인 경우
     * @throws IllegalArgumentException birthDate가 baseDate보다 이후인 경우
     */
    public static int calculateAge(LocalDate birthDate, LocalDate baseDate) {
        if (birthDate == null || baseDate == null) {
            throw new IllegalArgumentException("생년월일과 기준일은 null일 수 없습니다.");
        }
        if (birthDate.isAfter(baseDate)) {
            throw new IllegalArgumentException(
                    "생년월일(" + birthDate + ")이 기준일(" + baseDate + ")보다 이후일 수 없습니다.");
        }
        return (int) ChronoUnit.YEARS.between(birthDate, baseDate);
    }

    /**
     * 병역기간을 연 단위로 계산합니다. (최대 6년)
     *
     * <p>세법상 병역기간 공제 시 최대 6년까지만 인정됩니다.</p>
     * <p>연 단위 계산 시 소수점 이하는 포함하여 반환합니다 (일수 기반 계산).</p>
     *
     * <p>예시:</p>
     * <ul>
     *   <li>start=2020-01-01, end=2021-10-01 → 1 (1년)</li>
     *   <li>start=2015-01-01, end=2024-01-01 → 6 (최대 6년 제한 적용)</li>
     * </ul>
     *
     * @param start 병역 시작일 (LocalDate)
     * @param end   병역 종료일 (LocalDate)
     * @return 병역기간 연수 (long, 최대 6년)
     * @throws IllegalArgumentException start 또는 end가 null인 경우
     * @throws IllegalArgumentException start가 end보다 이후인 경우
     */
    public static long calculateMilitaryYears(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("병역 시작일과 종료일은 null일 수 없습니다.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "병역 시작일(" + start + ")이 종료일(" + end + ")보다 이후일 수 없습니다.");
        }
        long years = ChronoUnit.YEARS.between(start, end);
        return Math.min(years, MAX_MILITARY_YEARS);
    }
}
