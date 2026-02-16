package com.entec.tax.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;

/**
 * JPA 설정.
 * <p>
 * JPA 리포지토리 스캔, 트랜잭션 관리, JPA Auditing 을 활성화한다.
 * </p>
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.entec.tax.domain.*.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {

    /**
     * JPA 트랜잭션 매니저 빈 등록.
     *
     * @param entityManagerFactory EntityManagerFactory (Spring Boot 자동 구성)
     * @return PlatformTransactionManager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
