package com.retailer.rewards.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Top level payload returned by the reward endpoints.
 *
 * <p>Carries the customer's identity, the period that was actually evaluated, the total
 * points, the per month breakdown and the underlying transactions.</p>
 */
@Schema(description = "Reward points earned by one customer over a period")
public class CustomerRewardResponse {

    @Schema(example = "1")
    private final Long customerId;

    @Schema(example = "Alice Johnson")
    private final String customerName;

    @Schema(example = "alice.johnson@example.com")
    private final String email;

    @Schema(example = "2021-03-18")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate memberSince;

    @Schema(description = "First day of the evaluated period, inclusive", example = "2026-06-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate periodStart;

    @Schema(description = "Last day of the evaluated period, inclusive", example = "2026-09-02")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate periodEnd;

    @Schema(example = "1204")
    private final int totalPoints;

    private final List<MonthlyRewardSummary> monthlyBreakdown;

    private final RewardSummary summary;

    private final List<TransactionDetail> transactions;

    public CustomerRewardResponse(Long customerId, String customerName, String email,
                                  LocalDate memberSince, LocalDate periodStart, LocalDate periodEnd,
                                  int totalPoints, List<MonthlyRewardSummary> monthlyBreakdown,
                                  RewardSummary summary, List<TransactionDetail> transactions) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.memberSince = memberSince;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalPoints = totalPoints;
        this.monthlyBreakdown = monthlyBreakdown;
        this.summary = summary;
        this.transactions = transactions;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getMemberSince() {
        return memberSince;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public List<MonthlyRewardSummary> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }

    public RewardSummary getSummary() {
        return summary;
    }

    public List<TransactionDetail> getTransactions() {
        return transactions;
    }
}
