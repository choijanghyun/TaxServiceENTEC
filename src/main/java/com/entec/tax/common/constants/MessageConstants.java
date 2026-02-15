package com.entec.tax.common.constants;

/**
 * 시스템 메시지 상수 정의
 * - 알림창 문구, 안내 메시지, 확인 메시지 등 UI에 표시되는 메시지
 */
public final class MessageConstants {

    private MessageConstants() {
        throw new IllegalStateException("상수 클래스는 인스턴스를 생성할 수 없습니다.");
    }

    // === 공통 처리 결과 메시지 ===
    public static final String MSG_SAVE_SUCCESS = "저장되었습니다.";
    public static final String MSG_SAVE_FAIL = "저장에 실패하였습니다.";
    public static final String MSG_UPDATE_SUCCESS = "수정되었습니다.";
    public static final String MSG_UPDATE_FAIL = "수정에 실패하였습니다.";
    public static final String MSG_DELETE_SUCCESS = "삭제되었습니다.";
    public static final String MSG_DELETE_FAIL = "삭제에 실패하였습니다.";
    public static final String MSG_SEARCH_NO_RESULT = "검색 결과가 없습니다.";

    // === 확인 메시지 ===
    public static final String MSG_CONFIRM_SAVE = "저장하시겠습니까?";
    public static final String MSG_CONFIRM_UPDATE = "수정하시겠습니까?";
    public static final String MSG_CONFIRM_DELETE = "삭제하시겠습니까? 삭제된 데이터는 복구할 수 없습니다.";
    public static final String MSG_CONFIRM_SUBMIT = "제출하시겠습니까? 제출 후에는 수정이 불가합니다.";

    // === 유효성 검증 메시지 ===
    public static final String MSG_REQUIRED = "필수 입력 항목입니다.";
    public static final String MSG_INVALID_FORMAT = "입력 형식이 올바르지 않습니다.";
    public static final String MSG_INVALID_DATE = "날짜 형식이 올바르지 않습니다. (예: 2024-01-01)";
    public static final String MSG_INVALID_NUMBER = "숫자만 입력 가능합니다.";
    public static final String MSG_INVALID_BIZ_NO = "사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)";
    public static final String MSG_INVALID_CORP_NO = "법인등록번호 형식이 올바르지 않습니다. (예: 110111-1234567)";
    public static final String MSG_INVALID_EMAIL = "이메일 형식이 올바르지 않습니다.";
    public static final String MSG_INVALID_PHONE = "전화번호 형식이 올바르지 않습니다.";
    public static final String MSG_MAX_LENGTH_EXCEEDED = "최대 입력 길이를 초과하였습니다.";

    // === 인증/권한 메시지 ===
    public static final String MSG_LOGIN_REQUIRED = "로그인이 필요합니다.";
    public static final String MSG_LOGIN_SUCCESS = "로그인되었습니다.";
    public static final String MSG_LOGIN_FAIL = "아이디 또는 비밀번호가 올바르지 않습니다.";
    public static final String MSG_LOGOUT_SUCCESS = "로그아웃되었습니다.";
    public static final String MSG_SESSION_EXPIRED = "세션이 만료되었습니다. 다시 로그인해 주세요.";
    public static final String MSG_ACCESS_DENIED = "접근 권한이 없습니다. 관리자에게 문의하세요.";

    // === 법인세 업무 메시지 ===
    public static final String MSG_TAX_CORP_NOT_FOUND = "법인 정보를 찾을 수 없습니다. 사업자등록번호를 확인해 주세요.";
    public static final String MSG_TAX_PERIOD_INVALID = "사업연도가 유효하지 않습니다. 사업연도를 확인해 주세요.";
    public static final String MSG_TAX_ALREADY_FILED = "해당 사업연도에 이미 신고된 건이 존재합니다.";
    public static final String MSG_TAX_CORRECTION_NOT_ALLOWED = "경정청구가 허용되지 않는 상태입니다.";
    public static final String MSG_TAX_CORRECTION_DEADLINE = "경정청구 기한이 초과되었습니다. (법정신고기한 경과 후 5년 이내)";
    public static final String MSG_TAX_CALCULATION_COMPLETE = "세액 계산이 완료되었습니다.";
    public static final String MSG_TAX_SUBMIT_SUCCESS = "경정청구서가 제출되었습니다.";
    public static final String MSG_TAX_SUBMIT_CONFIRM = "경정청구서를 제출하시겠습니까? 제출 후 수정이 불가합니다.";
    public static final String MSG_TAX_DATA_INCOMPLETE = "필수 신고 데이터가 누락되었습니다. 입력 항목을 확인해 주세요.";

    // === 파일 관련 메시지 ===
    public static final String MSG_FILE_UPLOAD_SUCCESS = "파일이 업로드되었습니다.";
    public static final String MSG_FILE_UPLOAD_FAIL = "파일 업로드에 실패하였습니다.";
    public static final String MSG_FILE_SIZE_EXCEEDED = "파일 크기가 제한(10MB)을 초과하였습니다.";
    public static final String MSG_FILE_TYPE_NOT_ALLOWED = "허용되지 않은 파일 형식입니다. (허용: pdf, xlsx, xls, csv, hwp, doc, docx, zip)";
    public static final String MSG_FILE_NOT_FOUND = "파일을 찾을 수 없습니다.";
    public static final String MSG_FILE_DOWNLOAD_FAIL = "파일 다운로드에 실패하였습니다.";

    // === 시스템 메시지 ===
    public static final String MSG_SYSTEM_ERROR = "시스템 오류가 발생하였습니다. 잠시 후 다시 시도해 주세요.";
    public static final String MSG_NETWORK_ERROR = "네트워크 오류가 발생하였습니다. 인터넷 연결을 확인해 주세요.";
    public static final String MSG_MAINTENANCE = "시스템 점검 중입니다. 점검 시간을 확인해 주세요.";
}
