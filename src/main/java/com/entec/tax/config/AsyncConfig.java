package com.entec.tax.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정.
 * <p>
 * {@code @Async} 어노테이션 사용 시 적용되는 ThreadPoolTaskExecutor 를 구성한다.
 * 경정청구 산출 처리 등 시간이 소요되는 비동기 작업에 활용된다.
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final int QUEUE_CAPACITY = 100;
    private static final String THREAD_NAME_PREFIX = "TaxAsync-";

    /**
     * 기본 비동기 작업 Executor.
     * <p>
     * {@code @Async} 어노테이션에 별도 Executor 이름을 지정하지 않으면 이 빈이 사용된다.
     * </p>
     *
     * @return Executor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Async TaskExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);

        return executor;
    }

    /**
     * 산출 엔진 전용 Executor.
     * <p>
     * 환급액 산출 작업은 연산 부하가 높으므로 별도 스레드 풀을 사용한다.
     * {@code @Async("calculationExecutor")} 로 지정하여 사용한다.
     * </p>
     *
     * @return Executor
     */
    @Bean(name = "calculationExecutor")
    public Executor calculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("TaxCalc-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();

        log.info("Calculation Executor initialized: corePoolSize=2, maxPoolSize=8, queueCapacity=50");

        return executor;
    }
}
