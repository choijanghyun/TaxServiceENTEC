package com.entec.tax.common.constants;

/**
 * 법인세 업무 관련 상수 정의
 * - 법인세 경정청구 업무에서 사용되는 업무 코드 및 상수값
 */
public final class TaxConstants {

    private TaxConstants() {
        throw new IllegalStateException("상수 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // === 신고 구분 코드 ===
    /** 확정신고 */
    public static final String FILING_TYPE_FINAL = "01";
    /** 수정신고 */
    public static final String FILING_TYPE_AMENDED = "02";
    /** 경정청구 */
    public static final String FILING_TYPE_CORRECTION = "03";
    /** 기한후신고 */
    public static final String FILING_TYPE_LATE = "04";

    // === 경정청구 상태 코드 ===
    /** 작성중 */
    public static final String CORRECTION_STATUS_DRAFT = "10";
    /** 검토중 */
    public static final String CORRECTION_STATUS_REVIEW = "20";
    /** 승인완료 */
    public static final String CORRECTION_STATUS_APPROVED = "30";
    /** 제출완료 */
    public static final String CORRECTION_STATUS_SUBMITTED = "40";
    /** 처리완료 */
    public static final String CORRECTION_STATUS_COMPLETED = "50";
    /** 반려 */
    public static final String CORRECTION_STATUS_REJECTED = "90";

    // === 세금 유형 코드 ===
    /** 법인세 */
    public static final String TAX_TYPE_CORPORATE = "CORP";
    /** 지방소득세 */
    public static final String TAX_TYPE_LOCAL_INCOME = "LOCAL";
    /** 농어촌특별세 */
    public static final String TAX_TYPE_RURAL = "RURAL";

    // === 사업연도 관련 ===
    /** 경정청구 가능 기한 (년) - 법정신고기한 경과 후 5년 이내 */
    public static final int CORRECTION_DEADLINE_YEARS = 5;
    /** 법인세 신고 기한 (월) - 사업연도 종료 후 3개월 */
    public static final int FILING_DEADLINE_MONTHS = 3;

    // === 세율 관련 상수 (2024년 기준) ===
    /** 2억 이하 세율 */
    public static final double TAX_RATE_LEVEL1 = 0.09;
    /** 2억 초과 ~ 200억 이하 세율 */
    public static final double TAX_RATE_LEVEL2 = 0.19;
    /** 200억 초과 ~ 3,000억 이하 세율 */
    public static final double TAX_RATE_LEVEL3 = 0.21;
    /** 3,000억 초과 세율 */
    public static final double TAX_RATE_LEVEL4 = 0.24;

    /** 2억 이하 기준금액 */
    public static final long TAX_BRACKET_LEVEL1 = 200000000L;
    /** 200억 이하 기준금액 */
    public static final long TAX_BRACKET_LEVEL2 = 20000000000L;
    /** 3,000억 이하 기준금액 */
    public static final long TAX_BRACKET_LEVEL3 = 300000000000L;

    // === 법인 구분 코드 ===
    /** 내국법인 */
    public static final String CORP_TYPE_DOMESTIC = "01";
    /** 외국법인 */
    public static final String CORP_TYPE_FOREIGN = "02";
    /** 비영리법인 */
    public static final String CORP_TYPE_NONPROFIT = "03";

    // === 법인 규모 구분 ===
    /** 대기업 */
    public static final String CORP_SIZE_LARGE = "L";
    /** 중견기업 */
    public static final String CORP_SIZE_MEDIUM = "M";
    /** 중소기업 */
    public static final String CORP_SIZE_SMALL = "S";

    // === 세무서 코드 체계 (주요 세무서) ===
    public static final String TAX_OFFICE_JONGNO = "110";
    public static final String TAX_OFFICE_JUNGGU = "111";
    public static final String TAX_OFFICE_YONGSAN = "112";
    public static final String TAX_OFFICE_SEONGDONG = "113";
    public static final String TAX_OFFICE_GANGNAM = "214";

    // === 별지 서식 코드 ===
    /** 법인세 과세표준 및 세액신고서 */
    public static final String FORM_TAX_RETURN = "FORM_001";
    /** 소득금액조정합계표 */
    public static final String FORM_INCOME_ADJ = "FORM_002";
    /** 자본금과 적립금 조정명세서 */
    public static final String FORM_CAPITAL_ADJ = "FORM_003";
    /** 세액공제·감면 신청서 */
    public static final String FORM_TAX_CREDIT = "FORM_004";
    /** 경정청구서 */
    public static final String FORM_CORRECTION_REQ = "FORM_005";
}
