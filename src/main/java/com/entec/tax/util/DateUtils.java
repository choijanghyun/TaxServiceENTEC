package com.entec.tax.util;

import com.entec.tax.common.constants.CommonConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 날짜/시간 유틸리티 클래스
 * - 날짜 포맷팅, 파싱, 변환, 계산 등의 범용 기능 제공
 */
public final class DateUtils {

    private DateUtils() {
        throw new IllegalStateException("유틸리티 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    private static final ZoneId ZONE_SEOUL = ZoneId.of(CommonConstants.TIME_ZONE);

    // === 포맷팅 ===

    /**
     * LocalDate를 지정된 패턴 문자열로 변환
     * @param date 변환할 날짜
     * @param pattern 날짜 패턴 (예: "yyyy-MM-dd")
     * @return 포맷팅된 문자열, date가 null이면 빈 문자열
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null || pattern == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * LocalDateTime을 지정된 패턴 문자열로 변환
     * @param dateTime 변환할 일시
     * @param pattern 날짜시간 패턴 (예: "yyyy-MM-dd HH:mm:ss")
     * @return 포맷팅된 문자열
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * LocalDate를 기본 포맷(yyyy-MM-dd)으로 변환
     */
    public static String formatDate(LocalDate date) {
        return format(date, CommonConstants.DATE_FORMAT);
    }

    /**
     * LocalDateTime을 기본 포맷(yyyy-MM-dd HH:mm:ss)으로 변환
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return format(dateTime, CommonConstants.DATE_TIME_FORMAT);
    }

    /**
     * LocalDate를 한국어 포맷(yyyy년 MM월 dd일)으로 변환
     */
    public static String formatKorean(LocalDate date) {
        return format(date, CommonConstants.DATE_FORMAT_KOREAN);
    }

    /**
     * LocalDate를 압축 포맷(yyyyMMdd)으로 변환
     */
    public static String formatCompact(LocalDate date) {
        return format(date, CommonConstants.DATE_FORMAT_COMPACT);
    }

    // === 파싱 ===

    /**
     * 문자열을 LocalDate로 파싱
     * @param dateStr 날짜 문자열
     * @param pattern 날짜 패턴
     * @return 파싱된 LocalDate, 실패 시 null
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.trim().isEmpty() || pattern == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 문자열을 기본 포맷(yyyy-MM-dd)으로 LocalDate 파싱
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, CommonConstants.DATE_FORMAT);
    }

    /**
     * 압축 포맷(yyyyMMdd) 문자열을 LocalDate로 파싱
     */
    public static LocalDate parseDateCompact(String dateStr) {
        return parseDate(dateStr, CommonConstants.DATE_FORMAT_COMPACT);
    }

    /**
     * 문자열을 LocalDateTime으로 파싱
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty() || pattern == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 기본 포맷(yyyy-MM-dd HH:mm:ss) 문자열을 LocalDateTime으로 파싱
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTime(dateTimeStr, CommonConstants.DATE_TIME_FORMAT);
    }

    // === 변환 ===

    /**
     * Date를 LocalDate로 변환
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZONE_SEOUL).toLocalDate();
    }

    /**
     * Date를 LocalDateTime으로 변환
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZONE_SEOUL).toLocalDateTime();
    }

    /**
     * LocalDate를 Date로 변환
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZONE_SEOUL).toInstant());
    }

    /**
     * LocalDateTime을 Date로 변환
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZONE_SEOUL).toInstant());
    }

    // === 계산 ===

    /**
     * 현재 날짜 (서울 시간 기준)
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE_SEOUL);
    }

    /**
     * 현재 일시 (서울 시간 기준)
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_SEOUL);
    }

    /**
     * 두 날짜 사이의 일수 차이 계산
     */
    public static long daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(from, to);
    }

    /**
     * 두 날짜 사이의 월수 차이 계산
     */
    public static long monthsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(from, to);
    }

    /**
     * 두 날짜 사이의 연수 차이 계산
     */
    public static long yearsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(from, to);
    }

    /**
     * 해당 연도의 첫째 날 반환
     */
    public static LocalDate firstDayOfYear(int year) {
        return LocalDate.of(year, 1, 1);
    }

    /**
     * 해당 연도의 마지막 날 반환
     */
    public static LocalDate lastDayOfYear(int year) {
        return LocalDate.of(year, 12, 31);
    }

    /**
     * 해당 월의 첫째 날 반환
     */
    public static LocalDate firstDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(1);
    }

    /**
     * 해당 월의 마지막 날 반환
     */
    public static LocalDate lastDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    // === 사업연도 관련 ===

    /**
     * 사업연도 시작일 생성 (기본: 1월 1일)
     * @param year 사업연도
     * @return 사업연도 시작일
     */
    public static LocalDate fiscalYearStart(int year) {
        return LocalDate.of(year, 1, 1);
    }

    /**
     * 사업연도 종료일 생성 (기본: 12월 31일)
     * @param year 사업연도
     * @return 사업연도 종료일
     */
    public static LocalDate fiscalYearEnd(int year) {
        return LocalDate.of(year, 12, 31);
    }

    /**
     * 법인세 신고 기한 계산 (사업연도 종료 후 3개월)
     * @param fiscalYearEnd 사업연도 종료일
     * @return 법인세 신고 기한
     */
    public static LocalDate filingDeadline(LocalDate fiscalYearEnd) {
        if (fiscalYearEnd == null) {
            return null;
        }
        return fiscalYearEnd.plusMonths(3);
    }

    /**
     * 경정청구 기한 계산 (법정신고기한 경과 후 5년 이내)
     * @param fiscalYearEnd 사업연도 종료일
     * @return 경정청구 기한
     */
    public static LocalDate correctionDeadline(LocalDate fiscalYearEnd) {
        if (fiscalYearEnd == null) {
            return null;
        }
        LocalDate filingDeadline = filingDeadline(fiscalYearEnd);
        return filingDeadline.plusYears(5);
    }

    /**
     * 경정청구 기한 이내인지 확인
     * @param fiscalYearEnd 사업연도 종료일
     * @return 경정청구 가능 여부
     */
    public static boolean isCorrectionAllowed(LocalDate fiscalYearEnd) {
        if (fiscalYearEnd == null) {
            return false;
        }
        LocalDate deadline = correctionDeadline(fiscalYearEnd);
        return !today().isAfter(deadline);
    }

    // === 검증 ===

    /**
     * 날짜 문자열 유효성 검증
     */
    public static boolean isValidDate(String dateStr, String pattern) {
        return parseDate(dateStr, pattern) != null;
    }

    /**
     * 기본 포맷(yyyy-MM-dd) 날짜 문자열 유효성 검증
     */
    public static boolean isValidDate(String dateStr) {
        return isValidDate(dateStr, CommonConstants.DATE_FORMAT);
    }
}
