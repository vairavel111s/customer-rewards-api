package com.retailer.rewards.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reward points earned by one customer within a single calendar month.
 *
 * <p>Months inside the requested period with no activity are still present with zero
 * counts, so a caller can render a complete timeline without filling gaps itself.</p>
 */
@Schema(description = "Reward points earned in one calendar month")
public class MonthlyRewardSummary {

    @Schema(example = "2026-07")
    private final String period;

    @Schema(example = "JULY")
    private final String month;

    @Schema(example = "7")
    private final int monthNumber;

    @Schema(example = "2026")
    private final int year;

    @Schema(example = "3")
    private final int transactionCount;

    @Schema(example = "310.50")
    private final BigDecimal amountSpent;

    @Schema(example = "290")
    private final int pointsEarned;

    public MonthlyRewardSummary(String period, String month, int monthNumber, int year,
                                int transactionCount, BigDecimal amountSpent, int pointsEarned) {
        this.period = period;
        this.month = month;
        this.monthNumber = monthNumber;
        this.year = year;
        this.transactionCount = transactionCount;
        this.amountSpent = amountSpent;
        this.pointsEarned = pointsEarned;
    }

    public String getPeriod() {
        return period;
    }

    public String getMonth() {
        return month;
    }

    public int getMonthNumber() {
        return monthNumber;
    }

    public int getYear() {
        return year;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getAmountSpent() {
        return amountSpent;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }
}
