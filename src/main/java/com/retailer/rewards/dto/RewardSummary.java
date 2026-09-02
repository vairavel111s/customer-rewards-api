package com.retailer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregate spending statistics for a customer over the requested period.
 *
 * <p>These figures are not needed to answer "how many points", but they give the caller a
 * far clearer picture of the customer's behaviour without a second round trip.</p>
 */
@Schema(description = "Aggregate spending statistics across the requested period")
public class RewardSummary {

    @Schema(example = "12")
    private final int totalTransactions;

    @Schema(example = "1842.75")
    private final BigDecimal totalAmountSpent;

    @Schema(example = "153.56")
    private final BigDecimal averageTransactionAmount;

    @Schema(example = "999.99")
    private final BigDecimal highestTransactionAmount;

    @Schema(example = "3")
    private final int monthsCovered;

    @Schema(example = "2")
    private final int monthsWithActivity;

    @Schema(example = "2026-06-03")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate firstTransactionDate;

    @Schema(example = "2026-08-27")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate lastTransactionDate;

    public RewardSummary(int totalTransactions, BigDecimal totalAmountSpent,
                         BigDecimal averageTransactionAmount, BigDecimal highestTransactionAmount,
                         int monthsCovered, int monthsWithActivity,
                         LocalDate firstTransactionDate, LocalDate lastTransactionDate) {
        this.totalTransactions = totalTransactions;
        this.totalAmountSpent = totalAmountSpent;
        this.averageTransactionAmount = averageTransactionAmount;
        this.highestTransactionAmount = highestTransactionAmount;
        this.monthsCovered = monthsCovered;
        this.monthsWithActivity = monthsWithActivity;
        this.firstTransactionDate = firstTransactionDate;
        this.lastTransactionDate = lastTransactionDate;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalAmountSpent() {
        return totalAmountSpent;
    }

    public BigDecimal getAverageTransactionAmount() {
        return averageTransactionAmount;
    }

    public BigDecimal getHighestTransactionAmount() {
        return highestTransactionAmount;
    }

    public int getMonthsCovered() {
        return monthsCovered;
    }

    public int getMonthsWithActivity() {
        return monthsWithActivity;
    }

    public LocalDate getFirstTransactionDate() {
        return firstTransactionDate;
    }

    public LocalDate getLastTransactionDate() {
        return lastTransactionDate;
    }
}
