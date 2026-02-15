package com.entec.tax.common.constants;

/**
 * 공통 상수 정의
 * - 시스템 전반에서 사용되는 공통 상수값
 */
public final class CommonConstants {

    private CommonConstants() {
        throw new IllegalStateException("상수 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // === 사용 여부 ===
    public static final String USE_Y = "Y";
    public static final String USE_N = "N";

    // === 작업 유형 (CRUD) ===
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_READ = "READ";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    // === 처리 상태 ===
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETE = "COMPLETE";
    public static final String STATUS_CANCEL = "CANCEL";

    // === 날짜/시간 포맷 ===
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";
    public static final String DATE_TIME_FORMAT_COMPACT = "yyyyMMddHHmmss";
    public static final String DATE_FORMAT_KOREAN = "yyyy년 MM월 dd일";
    public static final String TIME_ZONE = "Asia/Seoul";

    // === 페이징 기본값 ===
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // === 파일 관련 ===
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_FILE_EXTENSIONS = {"pdf", "xlsx", "xls", "csv", "hwp", "doc", "docx", "zip"};
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    // === 인코딩 ===
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String CHARSET_EUCKR = "EUC-KR";

    // === 시스템 구분 ===
    public static final String SYSTEM_CODE = "TAX_SERVICE";
    public static final String SYSTEM_NAME = "법인세 경정청구 서비스";

    // === 구분자 ===
    public static final String DELIMITER_COMMA = ",";
    public static final String DELIMITER_PIPE = "|";
    public static final String DELIMITER_HYPHEN = "-";
    public static final String DELIMITER_UNDERSCORE = "_";
}
