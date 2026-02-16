package com.entec.tax.common.interceptor;

import com.entec.tax.common.dto.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * API Key 인증 및 요청 컨텍스트 설정 인터셉터.
 * <p>
 * 요청의 {@code X-API-Key} 헤더를 검증하고,
 * 요청자 정보(requestedBy, clientIp, traceId)를
 * {@link RequestContext} ThreadLocal 및 {@link MDC}에 설정한다.
 * </p>
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyInterceptor.class);

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUESTED_BY_HEADER = "X-Requested-By";

    @Value("${app.security.api-key:#{null}}")
    private String expectedApiKey;

    /**
     * 요청 전처리: API Key 검증 및 RequestContext 초기화.
     *
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @param handler  핸들러
     * @return 인증 성공 시 true, 실패 시 false
     * @throws Exception 처리 중 발생한 예외
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // API Key 검증 (설정되지 않은 경우 개발 환경으로 간주하여 통과)
        if (expectedApiKey != null && !expectedApiKey.trim().isEmpty()) {
            String requestApiKey = request.getHeader(API_KEY_HEADER);
            if (!expectedApiKey.equals(requestApiKey)) {
                log.warn("API Key 인증 실패: clientIp={}", getClientIp(request));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API Key\"}");
                return false;
            }
        }

        // 요청자 정보 추출
        String requestedBy = request.getHeader(REQUESTED_BY_HEADER);
        if (requestedBy == null || requestedBy.trim().isEmpty()) {
            requestedBy = "ANONYMOUS";
        }

        String clientIp = getClientIp(request);

        // traceId: 헤더에서 추출하거나 신규 생성
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // RequestContext ThreadLocal 초기화
        RequestContext.init(null, requestedBy, clientIp, traceId);

        // MDC 설정 (로깅 프레임워크 연동)
        MDC.put("traceId", traceId);
        MDC.put("requestedBy", requestedBy);
        MDC.put("clientIp", clientIp);

        return true;
    }

    /**
     * 요청 완료 후 ThreadLocal 및 MDC 정리.
     *
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @param handler  핸들러
     * @param ex       처리 중 발생한 예외 (없으면 null)
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        RequestContext.clear();
        MDC.clear();
    }

    // ------------------------------------------------------------------ Private helpers

    /**
     * 클라이언트 IP 주소를 추출한다.
     * 프록시/로드밸런서를 거친 경우 X-Forwarded-For 헤더를 우선 사용한다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 다중 프록시인 경우 첫 번째 IP가 원본 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
