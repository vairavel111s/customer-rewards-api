package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.retailer.rewards.config.RewardProperties;
import org.springframework.stereotype.Component;

/**
 * Applies the retailer's reward rule to a single transaction amount.
 *
 * <p>The rule is tiered:</p>
 * <ul>
 *   <li>2 points for every whole dollar spent <em>over</em> $100.</li>
 *   <li>1 point for every whole dollar spent <em>between</em> $50 and $100.</li>
 *   <li>Nothing at or below $50.</li>
 * </ul>
 *
 * <p>Worked example, a $120 purchase: {@code 2 x $20 + 1 x $50 = 90 points}.</p>
 *
 * <p>Partial dollars do not earn points, so the amount is truncated (not rounded) to whole
 * dollars first. A $50.99 purchase therefore earns nothing, the same as a $50.00 purchase.
 * This class holds no state beyond its injected configuration and is thread safe.</p>
 */
@Component
public class RewardCalculator {

    /**
     * Guards against absurd inputs overflowing the {@code int} point total. At the upper
     * tier rate this bound still leaves the result comfortably inside {@code int} range.
     */
    private static final BigDecimal MAX_SUPPORTED_AMOUNT = new BigDecimal("1000000000");

    private final RewardProperties properties;

    public RewardCalculator(RewardProperties properties) {
        this.properties = properties;
    }

    /**
     * Calculates the points earned by a single transaction.
     *
     * @param amount the transaction amount in dollars; {@code null}, zero and negative
     *               amounts (for example refunds) earn no points
     * @return the points earned, never negative
     * @throws IllegalArgumentException if the amount exceeds the supported range
     */
    public int calculatePoints(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        if (amount.compareTo(MAX_SUPPORTED_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Transaction amount exceeds the supported maximum of " + MAX_SUPPORTED_AMOUNT);
        }

        long wholeDollars = amount.setScale(0, RoundingMode.DOWN).longValueExact();

        long upperTierDollars = Math.max(0L, wholeDollars - properties.getUpperThreshold());
        long lowerTierDollars = Math.max(0L,
                Math.min(wholeDollars, properties.getUpperThreshold()) - properties.getLowerThreshold());

        long points = upperTierDollars * properties.getUpperTierPointsPerDollar()
                + lowerTierDollars * properties.getLowerTierPointsPerDollar();

        return Math.toIntExact(points);
    }
}
