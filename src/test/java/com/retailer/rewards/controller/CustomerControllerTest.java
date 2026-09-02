package com.retailer.rewards.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import com.retailer.rewards.dto.CustomerResponse;
import com.retailer.rewards.dto.PagedResponse;
import com.retailer.rewards.dto.TransactionDetail;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link CustomerController}.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("lists every customer")
    void listsCustomers() throws Exception {
        when(customerService.findAllCustomers()).thenReturn(Arrays.asList(
                new CustomerResponse(1L, "Alice Johnson", "alice.johnson@example.com",
                        LocalDate.of(2021, 3, 18), 7L),
                new CustomerResponse(2L, "Emily Watson", "emily.watson@example.com",
                        LocalDate.of(2024, 5, 6), 0L)));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].totalTransactions").value(7))
                .andExpect(jsonPath("$[1].totalTransactions").value(0));
    }

    @Test
    @DisplayName("returns a page of transactions with default paging")
    void returnsTransactionsWithDefaultPaging() throws Exception {
        PagedResponse<TransactionDetail> page = new PagedResponse<>(
                Collections.singletonList(new TransactionDetail(10L, LocalDate.of(2026, 6, 10),
                        new BigDecimal("120.00"), 90, "Electronics")),
                0, 20, 1L, 1, true);

        when(customerService.findTransactions(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/customers/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value(10))
                .andExpect(jsonPath("$.content[0].pointsEarned").value(90))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(customerService).findTransactions(1L, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("returns 404 for transactions of an unknown customer")
    void returnsNotFoundForUnknownCustomer() throws Exception {
        when(customerService.findTransactions(eq(99L), any()))
                .thenThrow(new CustomerNotFoundException(99L));

        mockMvc.perform(get("/api/v1/customers/99/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("rejects a page size above the allowed maximum")
    void rejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1/transactions").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("rejects a negative page number")
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1/transactions").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
