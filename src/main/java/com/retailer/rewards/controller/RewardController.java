package com.retailer.rewards.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.dto.ErrorResponse;
import com.retailer.rewards.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for reward point calculation.
 *
 * <p>The date range is optional on every endpoint. Omitting it evaluates the default three
 * calendar month window ending today, which is the scenario described in the requirements;
 * supplying it makes the same endpoint work for any period the caller needs.</p>
 */
@RestController
@RequestMapping("/api/v1/rewards")
@Validated
@Tag(name = "Rewards", description = "Reward point calculation")
public class RewardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewardController.class);

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping("/customers/{customerId}")
    @Operation(summary = "Reward points for one customer",
            description = "Returns total and per month reward points for a customer, along with "
                    + "the transactions that produced them.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rewards calculated"),
            @ApiResponse(responseCode = "400", description = "Invalid customer id or date range",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerRewardResponse> getRewardsForCustomer(
            @Parameter(description = "Customer identifier", example = "1")
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId,

            @Parameter(description = "Inclusive start date (yyyy-MM-dd). Defaults to the start of "
                    + "the three month window.", example = "2026-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Inclusive end date (yyyy-MM-dd). Defaults to today.",
                    example = "2026-08-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LOGGER.info("GET rewards for customer {} startDate={} endDate={}",
                customerId, startDate, endDate);
        return ResponseEntity.ok(
                rewardService.calculateRewardsForCustomer(customerId, startDate, endDate));
    }

    @GetMapping("/customers")
    @Operation(summary = "Reward points for every customer",
            description = "Same calculation as the single customer endpoint, applied across all "
                    + "customers. Customers with no activity are returned with zero totals.")
    public ResponseEntity<List<CustomerRewardResponse>> getRewardsForAllCustomers(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LOGGER.info("GET rewards for all customers startDate={} endDate={}", startDate, endDate);
        return ResponseEntity.ok(
                rewardService.calculateRewardsForAllCustomers(startDate, endDate));
    }

    @GetMapping("/customers/{customerId}/async")
    @Operation(summary = "Reward points for one customer, fetched asynchronously",
            description = "Identical payload to the synchronous endpoint, but the work runs on a "
                    + "dedicated executor behind a simulated downstream delay. The request thread "
                    + "is released while the calculation is in flight.")
    public CompletableFuture<ResponseEntity<CustomerRewardResponse>> getRewardsForCustomerAsync(
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LOGGER.info("GET (async) rewards for customer {} startDate={} endDate={}",
                customerId, startDate, endDate);
        return rewardService.calculateRewardsForCustomerAsync(customerId, startDate, endDate)
                .thenApply(ResponseEntity::ok);
    }
}
