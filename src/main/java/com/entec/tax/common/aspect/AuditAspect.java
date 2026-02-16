package com.entec.tax.common.aspect;

import com.entec.tax.common.annotation.Auditable;
import com.entec.tax.common.dto.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 감사추적 Aspect.
 * <p>
 * {@link Auditable} 어노테이션이 선언된 메서드의 실행 정보를
 * {@code LOG_CALCULATION} 테이블에 자동 기록한다.
 * </p>
 *
 * <p>기록 항목:</p>
 * <ul>
 *   <li>req_id — 메서드 파라미터 또는 RequestContext 에서 추출</li>
 *   <li>calc_step — {@link Auditable#step()}</li>
 *   <li>function_name — 클래스명.메서드명</li>
 *   <li>input_data — 메서드 인자 JSON</li>
 *   <li>output_data — 메서드 반환값 JSON</li>
 *   <li>executed_at — 실행 시작 시각</li>
 *   <li>duration_ms — 소요 시간(밀리초)</li>
 *   <li>trace_id — MDC 또는 RequestContext 에서 추출</li>
 * </ul>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private static final String INSERT_SQL =
            "INSERT INTO LOG_CALCULATION " +
            "(req_id, calc_step, function_name, input_data, output_data, " +
            " log_level, executed_at, duration_ms, trace_id, executed_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final int MAX_JSON_LENGTH = 65535;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * {@link Auditable} 어노테이션이 선언된 메서드를 감싸(around) 실행 정보를 기록한다.
     *
     * @param joinPoint 조인포인트
     * @param auditable 감사추적 어노테이션
     * @return 대상 메서드의 반환값
     * @throws Throwable 대상 메서드에서 발생한 예외
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        LocalDateTime executedAt = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        String reqId = extractReqId(joinPoint);
        String calcStep = auditable.step();
        String functionName = buildFunctionName(joinPoint);
        String inputData = serializeToJson(joinPoint.getArgs());

        Object result = null;
        String logLevel = "INFO";
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            logLevel = "ERROR";
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            String outputData = serializeToJson(result);
            String traceId = resolveTraceId();
            String executedBy = resolveExecutedBy();

            saveLog(reqId, calcStep, functionName, inputData, outputData,
                    logLevel, executedAt, durationMs, traceId, executedBy);
        }
    }

    // ------------------------------------------------------------------ Private helpers

    /**
     * 메서드 파라미터에서 reqId 를 추출한다.
     * 1차: 파라미터명이 "reqId" 인 String 파라미터
     * 2차: RequestContext ThreadLocal
     */
    private String extractReqId(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < parameters.length; i++) {
                if ("reqId".equals(parameters[i].getName()) && args[i] instanceof String) {
                    return (String) args[i];
                }
            }
        } catch (Exception e) {
            log.debug("reqId 파라미터 추출 실패, RequestContext 에서 조회합니다.", e);
        }

        RequestContext ctx = RequestContext.get();
        return ctx.getReqId();
    }

    /**
     * 클래스명.메서드명 형태의 함수명을 구성한다.
     */
    private String buildFunctionName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName;
    }

    /**
     * MDC 또는 RequestContext 에서 traceId 를 조회한다.
     */
    private String resolveTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }
        RequestContext ctx = RequestContext.get();
        return ctx.getTraceId();
    }

    /**
     * RequestContext 에서 실행자 정보를 조회한다.
     */
    private String resolveExecutedBy() {
        RequestContext ctx = RequestContext.get();
        return ctx.getRequestedBy();
    }

    /**
     * 객체를 JSON 문자열로 직렬화한다.
     * 직렬화 실패 시 null 을 반환한다.
     */
    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (json.length() > MAX_JSON_LENGTH) {
                json = json.substring(0, MAX_JSON_LENGTH);
            }
            return json;
        } catch (Exception e) {
            log.warn("감사추적 JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * LOG_CALCULATION 테이블에 감사 로그를 저장한다.
     * 저장 실패 시 예외를 로그로 기록하고, 비즈니스 로직에는 영향을 주지 않는다.
     */
    private void saveLog(String reqId, String calcStep, String functionName,
                         String inputData, String outputData, String logLevel,
                         LocalDateTime executedAt, long durationMs,
                         String traceId, String executedBy) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    reqId,
                    calcStep,
                    functionName,
                    inputData,
                    outputData,
                    logLevel,
                    Timestamp.valueOf(executedAt),
                    (int) durationMs,
                    traceId,
                    executedBy);
        } catch (Exception e) {
            log.error("LOG_CALCULATION 저장 실패: reqId={}, step={}, error={}",
                    reqId, calcStep, e.getMessage(), e);
        }
    }
}
