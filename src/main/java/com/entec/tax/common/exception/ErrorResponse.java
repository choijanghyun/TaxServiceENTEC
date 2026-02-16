package com.entec.tax.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 표준 에러 응답 DTO (Section 28.5 Error Response Schema)
 * <pre>
 * {
 *   "error": {
 *     "code": "ERR_XXX",
 *     "message": "...",
 *     "details": [...],
 *     "req_id": "...",
 *     "timestamp": "...",
 *     "trace_id": "..."
 *   }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @JsonProperty("error")
    private final ErrorBody error;

    private ErrorResponse(ErrorBody error) {
        this.error = error;
    }

    public ErrorBody getError() {
        return error;
    }

    /**
     * Builder 를 생성한다.
     */
    public static Builder builder() {
        return new Builder();
    }

    // ─── ErrorBody (inner class) ──────────────────────────────────────

    /**
     * 에러 응답 본문
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("code")
        private final String code;

        @JsonProperty("message")
        private final String message;

        @JsonProperty("details")
        private final List<Map<String, Object>> details;

        @JsonProperty("req_id")
        private final String reqId;

        @JsonProperty("timestamp")
        private final String timestamp;

        @JsonProperty("trace_id")
        private final String traceId;

        private ErrorBody(String code, String message, List<Map<String, Object>> details,
                          String reqId, String timestamp, String traceId) {
            this.code = code;
            this.message = message;
            this.details = details;
            this.reqId = reqId;
            this.timestamp = timestamp;
            this.traceId = traceId;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public List<Map<String, Object>> getDetails() {
            return details;
        }

        public String getReqId() {
            return reqId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getTraceId() {
            return traceId;
        }
    }

    // ─── Builder ──────────────────────────────────────────────────────

    public static class Builder {

        private String code;
        private String message;
        private List<Map<String, Object>> details;
        private String reqId;
        private String timestamp;
        private String traceId;

        private Builder() {
            // 기본 timestamp 를 현재 시각으로 설정
            this.timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(List<Map<String, Object>> details) {
            this.details = details != null
                    ? Collections.unmodifiableList(new ArrayList<Map<String, Object>>(details))
                    : null;
            return this;
        }

        public Builder reqId(String reqId) {
            this.reqId = reqId;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * {@link ErrorCode}로부터 code, message 를 자동 설정한다.
         */
        public Builder errorCode(ErrorCode errorCode) {
            this.code = errorCode.getCode();
            if (this.message == null) {
                this.message = errorCode.getDefaultMessage();
            }
            return this;
        }

        public ErrorResponse build() {
            ErrorBody body = new ErrorBody(code, message, details, reqId, timestamp, traceId);
            return new ErrorResponse(body);
        }
    }
}
