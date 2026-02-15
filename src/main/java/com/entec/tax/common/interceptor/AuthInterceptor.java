package com.entec.tax.common.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 인증/인가 인터셉터
 * - 요청 전처리: 인증 토큰 검증, 사용자 정보 설정
 * - 요청 후처리: 응답 로깅
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.debug("[AuthInterceptor] 요청 시작 - {} {}", method, requestURI);

        // OPTIONS 요청은 통과
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // TODO: 실제 인증 로직 구현 (JWT 토큰 검증 등)
        // String token = request.getHeader("Authorization");

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 응답 후처리 로직
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null) {
            log.error("[AuthInterceptor] 요청 처리 중 오류 발생 - {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
    }
}
