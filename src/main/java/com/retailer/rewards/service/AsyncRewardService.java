package com.retailer.rewards.service;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import com.retailer.rewards.config.AsyncConfig;
import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.dto.CustomerRewardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Non blocking wrapper around {@link RewardService}.
 *
 * <p>This lives in its own bean on purpose. Spring applies {@code @Transactional} through a
 * proxy, and a proxy is only consulted when a call arrives from outside the object. Had the
 * asynchronous method stayed inside {@link RewardServiceImpl} and called its own
 * {@code calculateRewardsForCustomer}, that self invocation would have bypassed the proxy
 * and run without the read only transaction the synchronous path gets, leaving the two
 * paths with different semantics. Calling across bean boundaries keeps them identical.</p>
 *
 * <p>The simulated downstream delay is applied here, before the delegate is called, so the
 * artificial latency never holds a database connection open.</p>
 */
@Service
public class AsyncRewardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRewardService.class);

    private final RewardService rewardService;
    private final RewardProperties properties;

    public AsyncRewardService(RewardService rewardService, RewardProperties properties) {
        this.rewardService = rewardService;
        this.properties = properties;
    }

    /**
     * Calculates a customer's rewards on the {@code rewards-async-*} pool.
     *
     * @return a future completing with the same payload the synchronous endpoint returns
     */
    @Async(AsyncConfig.REWARDS_EXECUTOR)
    public CompletableFuture<CustomerRewardResponse> calculateRewardsForCustomer(
            Long customerId, LocalDate requestedStart, LocalDate requestedEnd) {

        LOGGER.info("Async reward request for customer {} running on thread {}",
                customerId, Thread.currentThread().getName());

        simulateDownstreamLatency();

        CustomerRewardResponse response =
                rewardService.calculateRewardsForCustomer(customerId, requestedStart, requestedEnd);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Stands in for a slow remote data source so the async path is observable end to end.
     */
    private void simulateDownstreamLatency() {
        long latencyMs = properties.getAsyncSimulatedLatencyMs();
        if (latencyMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Simulated latency interrupted, continuing without the delay");
        }
    }
}
