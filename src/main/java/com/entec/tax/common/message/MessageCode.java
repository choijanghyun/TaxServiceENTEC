package com.entec.tax.common.message;

import java.text.MessageFormat;

/**
 * UI/시스템 메시지 코드.
 * <p>
 * 모든 사용자·시스템 메시지를 한곳에서 관리한다.
 * {@link #format(Object...)} 메서드를 통해 파라미터 치환된 메시지를 생성할 수 있다.
 * </p>
 */
public enum MessageCode {

    // === 성공 메시지 ===
    MSG_REQUEST_RECEIVED("MSG_001", "요청이 정상적으로 접수되었습니다. 요청번호: {0}"),
    MSG_ANALYSIS_STARTED("MSG_002", "점검 분석이 시작되었습니다."),
    MSG_ANALYSIS_COMPLETED("MSG_003", "점검 분석이 완료되었습니다. 총 수령예상액: {0}원"),
    MSG_REPORT_GENERATED("MSG_004", "보고서가 정상적으로 생성되었습니다."),

    // === 경고 메시지 ===
    WARN_SETTLEMENT_BLOCKED("WRN_001", "결산조정 항목이 포함되어 있습니다. 해당 항목은 경정청구 대상에서 자동 배제됩니다."),
    WARN_DEADLINE_APPROACHING("WRN_002", "경정청구 기한이 {0}일 남았습니다. 조속한 신청을 권장합니다."),
    WARN_SINCERITY_MISMATCH("WRN_003", "수동입력 성실신고대상 여부와 수입금액 기준 자동판정이 불일치합니다. 확인이 필요합니다."),
    WARN_CIRCULAR_NOT_CONVERGED("WRN_004", "최저한세 순환참조가 {0}회 반복 후 수렴하지 않았습니다. 현재까지 최선 결과를 사용합니다."),
    WARN_GREEDY_FALLBACK("WRN_005", "공제 항목 수({0}건)가 임계치를 초과하여 Greedy 폴백 모드로 전환되었습니다."),
    WARN_INDIRECT_SHARE_BELOW("WRN_006", "간접외국납부세액공제: {0} 자회사의 지분율이 25% 미만으로 공제 대상에서 제외됩니다."),

    // === 오류 메시지 ===
    ERR_INVALID_JSON_MSG("ERR_001", "입력 JSON 형식이 올바르지 않습니다."),
    ERR_MISSING_REQUIRED("ERR_002", "필수 카테고리 ''{0}''이(가) 누락되었습니다."),
    ERR_DUPLICATE_CATEGORY_MSG("ERR_003", "동일 카테고리 ''{0}''이(가) 중복 전송되었습니다."),
    ERR_REQUEST_NOT_FOUND_MSG("ERR_004", "요청번호 ''{0}''을(를) 찾을 수 없습니다."),
    ERR_INVALID_STATUS_MSG("ERR_005", "현재 상태 ''{0}''에서 요청한 작업을 수행할 수 없습니다."),
    ERR_HARD_FAIL_MSG("ERR_006", "결산확정 원칙 검증 실패: {0}. 경정청구 대상이 아닙니다."),
    ERR_CALCULATION_FAILED_MSG("ERR_007", "산출 엔진 오류: 단계 {0}에서 오류가 발생했습니다."),
    ERR_TIMEOUT_MSG("ERR_008", "처리 시간이 초과되었습니다 ({0}초). 현재까지 최선 결과를 반환합니다."),
    ERR_CLAIM_EXPIRED("ERR_009", "경정청구 기한(5년)이 경과하였습니다. 기한: {0}"),
    ERR_NOT_SME("ERR_010", "중소기업에 해당하지 않아 소급공제가 불가합니다."),

    // === 안내 메시지 ===
    INFO_LOCAL_TAX("INF_001", "법인세(종합소득세) 환급에 따른 지방소득세 별도 경정청구가 필요합니다. 예상 환급액: {0}원"),
    INFO_CARRYFORWARD("INF_002", "최저한세 초과분 {0}원은 향후 10년간 이월공제 가능합니다."),
    INFO_LOSS_CARRYBACK("INF_003", "소급공제 신청 시 환급 예상액: {0}원 (이월공제 현재가치: {1}원)"),
    INFO_BIZ_CAR_NOT_ELIGIBLE("INF_004", "차량 [{0}] — 임직원 전용보험 미가입으로 경정 대상이 아닙니다.");

    private final String code;
    private final String messageTemplate;

    MessageCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String getCode() {
        return code;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * 메시지 템플릿에 파라미터를 치환하여 최종 메시지 문자열을 반환한다.
     *
     * @param args 치환할 파라미터 ({0}, {1}, ... 순서)
     * @return 파라미터가 치환된 메시지 문자열
     */
    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        return MessageFormat.format(messageTemplate, args);
    }
}
