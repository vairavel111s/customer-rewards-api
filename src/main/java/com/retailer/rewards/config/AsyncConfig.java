package com.retailer.rewards.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Thread pool backing the asynchronous reward endpoints.
 *
 * <p>A dedicated, bounded executor is used rather than the Spring default so that a burst
 * of async requests cannot exhaust the container's request threads. When the queue is full
 * the caller thread runs the task, which applies natural back pressure instead of dropping
 * work on the floor.</p>
 */
@Configuration
public class AsyncConfig {

    public static final String REWARDS_EXECUTOR = "rewardsTaskExecutor";

    @Bean(name = REWARDS_EXECUTOR)
    public Executor rewardsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("rewards-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
