package com.retailer.rewards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised configuration for the rewards program.
 *
 * <p>Keeping the thresholds and rates out of the code means the business rule can be
 * adjusted per environment without recompiling, and lets the unit tests exercise
 * alternative rule sets.</p>
 */
@ConfigurationProperties(prefix = "rewards")
public class RewardProperties {

    /** Spend below this amount (in dollars) earns no points. */
    private int lowerThreshold = 50;

    /** Spend above this amount (in dollars) earns the higher rate. */
    private int upperThreshold = 100;

    /** Points awarded per dollar between the lower and upper thresholds. */
    private int lowerTierPointsPerDollar = 1;

    /** Points awarded per dollar above the upper threshold. */
    private int upperTierPointsPerDollar = 2;

    /** Calendar months covered when the caller supplies no date range. */
    private int defaultPeriodMonths = 3;

    /** Largest range a caller is allowed to request, in months. */
    private int maxPeriodMonths = 24;

    /** Artificial delay applied by the async endpoint to imitate a slow downstream call. */
    private long asyncSimulatedLatencyMs = 750L;

    public int getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(int lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    public int getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(int upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public int getLowerTierPointsPerDollar() {
        return lowerTierPointsPerDollar;
    }

    public void setLowerTierPointsPerDollar(int lowerTierPointsPerDollar) {
        this.lowerTierPointsPerDollar = lowerTierPointsPerDollar;
    }

    public int getUpperTierPointsPerDollar() {
        return upperTierPointsPerDollar;
    }

    public void setUpperTierPointsPerDollar(int upperTierPointsPerDollar) {
        this.upperTierPointsPerDollar = upperTierPointsPerDollar;
    }

    public int getDefaultPeriodMonths() {
        return defaultPeriodMonths;
    }

    public void setDefaultPeriodMonths(int defaultPeriodMonths) {
        this.defaultPeriodMonths = defaultPeriodMonths;
    }

    public int getMaxPeriodMonths() {
        return maxPeriodMonths;
    }

    public void setMaxPeriodMonths(int maxPeriodMonths) {
        this.maxPeriodMonths = maxPeriodMonths;
    }

    public long getAsyncSimulatedLatencyMs() {
        return asyncSimulatedLatencyMs;
    }

    public void setAsyncSimulatedLatencyMs(long asyncSimulatedLatencyMs) {
        this.asyncSimulatedLatencyMs = asyncSimulatedLatencyMs;
    }
}
