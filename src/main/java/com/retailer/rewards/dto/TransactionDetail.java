package com.retailer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One transaction as it appears in a reward response, together with the points it earned.
 * Including the per transaction points lets a caller reconcile the totals themselves.
 */
@Schema(description = "A single purchase and the reward points it earned")
public class TransactionDetail {

    @Schema(example = "42")
    private final Long transactionId;

    @Schema(example = "2026-07-14")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate transactionDate;

    @Schema(example = "120.00")
    private final BigDecimal amount;

    @Schema(example = "90")
    private final int pointsEarned;

    @Schema(example = "Electronics")
    private final String description;

    public TransactionDetail(Long transactionId, LocalDate transactionDate, BigDecimal amount,
                             int pointsEarned, String description) {
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.amount = amount;
        this.pointsEarned = pointsEarned;
        this.description = description;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public String getDescription() {
        return description;
    }
}
