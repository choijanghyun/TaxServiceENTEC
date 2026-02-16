package com.entec.tax.common.constants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 시스템 공통 상수
 */
public final class SystemConstants {

    private SystemConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ──────────────────────────────────────────────
    // 수치 계산 관련
    // ──────────────────────────────────────────────

    /** 10원 미만 절사 스케일 (-1 → 10^1 = 10원 단위) */
    public static final int TRUNCATION_SCALE = -1;

    /** 비율 소수점 자릿수 */
    public static final int RATE_SCALE = 4;

    /** 환급가산금 1원 미만 절사 */
    public static final int INTEREST_SCALE = 0;

    /** 기본 반올림 모드 (절사) */
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.DOWN;

    // ──────────────────────────────────────────────
    // 최적화 관련
    // ──────────────────────────────────────────────

    /** 조합 최적화 최대 반복 횟수 */
    public static final int MAX_COMBO_ITERATIONS = 5;

    /** 수렴 판정 임계값 (원) */
    public static final long CONVERGENCE_EPSILON = 1L;

    // ──────────────────────────────────────────────
    // 세율 관련
    // ──────────────────────────────────────────────

    /** 지방소득세율 10% */
    public static final BigDecimal LOCAL_TAX_RATE = new BigDecimal("0.10");

    /** 농어촌특별세율 20% */
    public static final BigDecimal NONGTEUKSE_RATE = new BigDecimal("0.20");

    // ──────────────────────────────────────────────
    // 요청 크기 제한
    // ──────────────────────────────────────────────

    /** 카테고리당 최대 JSON 크기 (10 MB) */
    public static final long MAX_JSON_SIZE_PER_CATEGORY = 10L * 1024 * 1024;

    /** 요청당 최대 카테고리 수 */
    public static final int MAX_CATEGORIES_PER_REQUEST = 40;

    /** 전체 최대 페이로드 크기 (50 MB) */
    public static final long MAX_PAYLOAD_SIZE = 50L * 1024 * 1024;

    // ──────────────────────────────────────────────
    // 요청 ID 관련
    // ──────────────────────────────────────────────

    /** 요청 ID 포맷: {taxType}-{applicantType}-{bizNo}-{seq} */
    public static final String REQ_ID_FORMAT = "%s-%s-%s-%03d";

    // ──────────────────────────────────────────────
    // 캐시 / 타임아웃 관련
    // ──────────────────────────────────────────────

    /** 기본 요청 타임아웃 (밀리초) - 5분 */
    public static final long DEFAULT_TIMEOUT_MS = 5L * 60 * 1000;

    /** 최대 재시도 횟수 */
    public static final int MAX_RETRY_COUNT = 3;

    /** 재시도 대기 간격 (밀리초) */
    public static final long RETRY_INTERVAL_MS = 1000L;

    // ──────────────────────────────────────────────
    // 기타
    // ──────────────────────────────────────────────

    /** 기본 페이징 크기 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 최대 페이징 크기 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 사업연도 기본 길이 (월) */
    public static final int DEFAULT_FISCAL_YEAR_MONTHS = 12;
}
