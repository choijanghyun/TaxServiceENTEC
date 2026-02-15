package com.entec.tax.common.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP 기반 공통 로깅
 * - Controller, Service, Repository 레이어의 메소드 실행 로깅
 * - 메소드 진입/종료, 실행 시간, 파라미터, 반환값 기록
 * - 예외 발생 시 에러 로깅
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Controller 레이어 포인트컷
     */
    @Pointcut("within(com.entec.tax..controller..*)")
    public void controllerPointcut() {
    }

    /**
     * Service 레이어 포인트컷
     */
    @Pointcut("within(com.entec.tax..service..*)")
    public void servicePointcut() {
    }

    /**
     * Repository/Mapper 레이어 포인트컷
     */
    @Pointcut("within(com.entec.tax..repository..*) || within(com.entec.tax..mapper..*)")
    public void repositoryPointcut() {
    }

    /**
     * Controller 메소드 실행 로깅 (Around)
     * - 요청 파라미터, 응답, 실행 시간 기록
     */
    @Around("controllerPointcut()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.info("[Controller] {}.{} 시작 - args={}", className, methodName, getArgs(joinPoint));

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[Controller] {}.{} 종료 - 소요시간={}ms", className, methodName, elapsedTime);
            return result;
        } catch (Throwable e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[Controller] {}.{} 오류 - 소요시간={}ms, error={}", className, methodName, elapsedTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Service 메소드 실행 로깅 (Around)
     * - 비즈니스 로직 실행 시간 추적
     */
    @Around("servicePointcut()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("[Service] {}.{} 시작", className, methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.debug("[Service] {}.{} 종료 - 소요시간={}ms", className, methodName, elapsedTime);
            return result;
        } catch (Throwable e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[Service] {}.{} 오류 - 소요시간={}ms, error={}", className, methodName, elapsedTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 예외 발생 시 로깅 (AfterThrowing)
     */
    @AfterThrowing(pointcut = "controllerPointcut() || servicePointcut() || repositoryPointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        log.error("[Exception] {}.{} - exception={}, message={}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
    }

    /**
     * 메소드 인자를 안전하게 문자열로 변환
     */
    private String getArgs(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "[]";
            }
            return Arrays.toString(args);
        } catch (Exception e) {
            return "[로깅 불가]";
        }
    }
}
