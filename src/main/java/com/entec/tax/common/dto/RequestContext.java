package com.entec.tax.common.dto;

/**
 * 요청 컨텍스트 ThreadLocal 홀더.
 * <p>
 * 현재 요청의 메타 정보(요청 ID, 요청자, 클라이언트 IP, 트레이스 ID)를
 * ThreadLocal 에 저장하여 서비스 계층 전반에서 접근할 수 있도록 한다.
 * 요청 처리가 완료되면 반드시 {@link #clear()}를 호출하여 메모리 누수를 방지해야 한다.
 * </p>
 */
public class RequestContext {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<RequestContext>();

    private String reqId;
    private String requestedBy;
    private String clientIp;
    private String traceId;

    public RequestContext() {
    }

    public RequestContext(String reqId, String requestedBy, String clientIp, String traceId) {
        this.reqId = reqId;
        this.requestedBy = requestedBy;
        this.clientIp = clientIp;
        this.traceId = traceId;
    }

    // ------------------------------------------------------------------ Static methods

    /**
     * 새로운 RequestContext 를 초기화하여 ThreadLocal 에 저장한다.
     *
     * @param reqId       요청 ID
     * @param requestedBy 요청자 식별자
     * @param clientIp    클라이언트 IP
     * @param traceId     트레이스 ID
     */
    public static void init(String reqId, String requestedBy, String clientIp, String traceId) {
        CONTEXT.set(new RequestContext(reqId, requestedBy, clientIp, traceId));
    }

    /**
     * 현재 스레드의 RequestContext 를 반환한다.
     * 초기화되지 않은 경우 빈 RequestContext 를 반환한다.
     *
     * @return 현재 RequestContext
     */
    public static RequestContext get() {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new RequestContext();
            CONTEXT.set(ctx);
        }
        return ctx;
    }

    /**
     * 현재 스레드의 RequestContext 를 설정한다.
     *
     * @param context 설정할 RequestContext
     */
    public static void set(RequestContext context) {
        CONTEXT.set(context);
    }

    /**
     * 현재 스레드의 RequestContext 를 제거한다.
     * 요청 처리 완료 후 반드시 호출하여 메모리 누수를 방지한다.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ------------------------------------------------------------------ Getters & Setters

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
