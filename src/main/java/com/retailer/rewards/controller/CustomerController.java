package com.retailer.rewards.controller;

import java.util.List;

import com.retailer.rewards.dto.CustomerResponse;
import com.retailer.rewards.dto.PagedResponse;
import com.retailer.rewards.dto.TransactionDetail;
import com.retailer.rewards.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for browsing customers and their raw transaction history.
 */
@RestController
@RequestMapping("/api/v1/customers")
@Validated
@Tag(name = "Customers", description = "Customer and transaction lookup")
public class CustomerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerController.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "List every customer",
            description = "Useful for discovering the customer ids available in the demo data set.")
    public ResponseEntity<List<CustomerResponse>> getCustomers() {
        LOGGER.info("GET customer list");
        return ResponseEntity.ok(customerService.findAllCustomers());
    }

    @GetMapping("/{customerId}/transactions")
    @Operation(summary = "Transaction history for one customer",
            description = "Paged, newest first, with the points each purchase earned.")
    public ResponseEntity<PagedResponse<TransactionDetail>> getTransactions(
            @PathVariable @Positive(message = "customerId must be a positive number") Long customerId,

            @RequestParam(defaultValue = "0")
            @PositiveOrZero(message = "page must not be negative") int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = MAX_PAGE_SIZE, message = "size must not exceed " + MAX_PAGE_SIZE) int size) {

        LOGGER.info("GET transactions for customer {} page={} size={}", customerId, page, size);
        return ResponseEntity.ok(
                customerService.findTransactions(customerId, PageRequest.of(page, size)));
    }
}
