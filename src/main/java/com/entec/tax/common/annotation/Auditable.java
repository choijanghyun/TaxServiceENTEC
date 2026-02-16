package com.entec.tax.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 추적 어노테이션.
 * <p>
 * 이 어노테이션이 선언된 메서드의 실행 정보(입력·출력·소요 시간 등)를
 * {@code LOG_CALCULATION} 테이블에 자동으로 기록한다.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 산출 단계 식별자 (예: "STEP_01", "STEP_02").
     */
    String step() default "";

    /**
     * 단계에 대한 설명.
     */
    String description() default "";
}
