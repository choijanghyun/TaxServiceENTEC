package com.entec.tax.common.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 입력 값 검증 실패 시 발생하는 예외
 * <p>
 * 각 필드별 검증 오류를 {@link FieldError} 목록으로 포함한다.
 */
public class ValidationException extends TaxServiceException {

    private static final long serialVersionUID = 1L;

    private final List<FieldError> fieldErrors;

    /**
     * @param errorCode   에러 코드
     * @param message     상세 메시지
     * @param reqId       요청 추적 ID
     * @param fieldErrors 필드별 검증 오류 목록
     */
    public ValidationException(ErrorCode errorCode, String message, String reqId,
                               List<FieldError> fieldErrors) {
        super(errorCode, message, reqId);
        this.fieldErrors = fieldErrors != null
                ? Collections.unmodifiableList(new ArrayList<FieldError>(fieldErrors))
                : Collections.<FieldError>emptyList();
    }

    /**
     * @param message     상세 메시지
     * @param reqId       요청 추적 ID
     * @param fieldErrors 필드별 검증 오류 목록
     */
    public ValidationException(String message, String reqId, List<FieldError> fieldErrors) {
        this(ErrorCode.VALIDATION_FAILED, message, reqId, fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * 개별 필드 검증 오류 정보
     */
    public static class FieldError {

        private final String field;
        private final String issue;
        private final String expected;
        private final String received;

        /**
         * @param field    오류가 발생한 필드명
         * @param issue    오류 내용 설명
         * @param expected 기대 값 또는 형식
         * @param received 실제 수신 값
         */
        public FieldError(String field, String issue, String expected, String received) {
            this.field = field;
            this.issue = issue;
            this.expected = expected;
            this.received = received;
        }

        public String getField() {
            return field;
        }

        public String getIssue() {
            return issue;
        }

        public String getExpected() {
            return expected;
        }

        public String getReceived() {
            return received;
        }
    }
}
