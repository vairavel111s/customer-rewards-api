package com.retailer.rewards.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A customer as returned by the customer listing endpoint.
 */
@Schema(description = "A customer enrolled in the rewards program")
public class CustomerResponse {

    @Schema(example = "1")
    private final Long customerId;

    @Schema(example = "Alice Johnson")
    private final String name;

    @Schema(example = "alice.johnson@example.com")
    private final String email;

    @Schema(example = "2021-03-18")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate memberSince;

    @Schema(description = "Lifetime transaction count, not limited to any period", example = "14")
    private final long totalTransactions;

    public CustomerResponse(Long customerId, String name, String email, LocalDate memberSince,
                            long totalTransactions) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.memberSince = memberSince;
        this.totalTransactions = totalTransactions;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getMemberSince() {
        return memberSince;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }
}
