package com.retailer.rewards.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the system clock as a bean.
 *
 * <p>Injecting a {@link Clock} instead of calling {@code LocalDate.now()} inline keeps the
 * date defaulting logic deterministic under test.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
