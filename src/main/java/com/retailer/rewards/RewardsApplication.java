package com.retailer.rewards;

import com.retailer.rewards.config.RewardProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the Customer Rewards API.
 *
 * <p>The service exposes RESTful endpoints that calculate the reward points a retailer
 * awards to its customers, broken down per calendar month and in total, over a caller
 * supplied time frame.</p>
 */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(RewardProperties.class)
public class RewardsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardsApplication.class, args);
    }
}
