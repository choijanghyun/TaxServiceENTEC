package com.entec.tax.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security 설정.
 * <p>
 * API Key 기반 인증을 적용한다. 클라이언트는 {@code X-API-Key} 헤더에
 * 유효한 API 키를 포함해야 보호 대상 엔드포인트에 접근할 수 있다.
 * 세션은 STATELESS 로 운영한다.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${app.security.api-key:#{null}}")
    private String apiKey;

    /**
     * SecurityFilterChain 빈 등록.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (Stateless REST API)
            .csrf().disable()

            // 세션 관리 - STATELESS
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

            // H2 Console iframe 허용 (개발 환경)
            .headers()
                .frameOptions().sameOrigin()
                .and()

            // 요청 인가 규칙
            .authorizeRequests()
                // Swagger UI / API docs 허용
                .antMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/v2/api-docs",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()
                // H2 Console 허용 (개발 환경)
                .antMatchers("/h2-console/**").permitAll()
                // Actuator health check 허용
                .antMatchers("/actuator/health", "/actuator/info").permitAll()
                // API 엔드포인트 - 인증 필요
                .antMatchers("/api/v1/**").authenticated()
                // 그 외 모든 요청 허용
                .anyRequest().permitAll()
                .and()

            // API Key 인증 필터 등록
            .addFilterBefore(apiKeyAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * API Key 인증 필터.
     * <p>
     * {@code X-API-Key} 헤더 값을 검증한다. 키가 설정되지 않은 경우(개발 환경)
     * 모든 요청을 통과시킨다.
     * </p>
     */
    @Bean
    public Filter apiKeyAuthenticationFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response,
                                 FilterChain chain) throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                String requestPath = httpRequest.getRequestURI();

                // API 경로가 아닌 경우 필터 통과
                if (!requestPath.startsWith("/api/v1/")) {
                    chain.doFilter(request, response);
                    return;
                }

                // API Key 가 설정되지 않은 경우 (개발 환경) 필터 통과
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    chain.doFilter(request, response);
                    return;
                }

                String requestApiKey = httpRequest.getHeader(API_KEY_HEADER);

                if (apiKey.equals(requestApiKey)) {
                    chain.doFilter(request, response);
                } else {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API Key\"}"
                    );
                }
            }
        };
    }
}
