package com.entec.tax.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정
 * - JPA Auditing 활성화 (BaseEntity의 생성일시/수정일시 자동 관리)
 * - JPA Repository 스캔 경로 설정
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.entec.tax")
public class JpaConfig {
}
