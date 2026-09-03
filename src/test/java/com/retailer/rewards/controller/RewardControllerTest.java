package com.retailer.rewards.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.dto.MonthlyRewardSummary;
import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.dto.TransactionDetail;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.service.AsyncRewardService;
import com.retailer.rewards.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer tests for {@link RewardController}.
 *
 * <p>The service is mocked, so these tests are about routing, parameter binding, the JSON
 * contract and the error mapping rather than about the reward arithmetic.</p>
 */
@WebMvcTest(RewardController.class)
class RewardControllerTest {

    private static final String BASE_PATH = "/api/v1/rewards/customers";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardService rewardService;

    @MockitoBean
    private AsyncRewardService asyncRewardService;

    @Test
    @DisplayName("returns the reward payload for a known customer")
    void returnsRewardsForCustomer() throws Exception {
        when(rewardService.calculateRewardsForCustomer(eq(1L), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_PATH + "/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.email").value("alice.johnson@example.com"))
                .andExpect(jsonPath("$.totalPoints").value(90))
                .andExpect(jsonPath("$.periodStart").value("2026-06-01"))
                .andExpect(jsonPath("$.periodEnd").value("2026-08-15"))
                .andExpect(jsonPath("$.monthlyBreakdown[0].month").value("JUNE"))
                .andExpect(jsonPath("$.monthlyBreakdown[0].pointsEarned").value(90))
                .andExpect(jsonPath("$.transactions[0].amount").value(120.00))
                .andExpect(jsonPath("$.transactions[0].pointsEarned").value(90))
                .andExpect(jsonPath("$.summary.totalTransactions").value(1));
    }

    @Test
    @DisplayName("passes an explicit date range through to the service")
    void forwardsExplicitDateRange() throws Exception {
        when(rewardService.calculateRewardsForCustomer(any(), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_PATH + "/1")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk());

        verify(rewardService).calculateRewardsForCustomer(
                1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("returns 404 when the customer does not exist")
    void returnsNotFoundForUnknownCustomer() throws Exception {
        when(rewardService.calculateRewardsForCustomer(eq(404L), any(), any()))
                .thenThrow(new CustomerNotFoundException(404L));

        mockMvc.perform(get(BASE_PATH + "/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No customer found with id 404"))
                .andExpect(jsonPath("$.path").value("/api/v1/rewards/customers/404"));
    }

    @Test
    @DisplayName("returns 400 when the date range is rejected by the service")
    void returnsBadRequestForInvalidRange() throws Exception {
        when(rewardService.calculateRewardsForCustomer(any(), any(), any()))
                .thenThrow(new InvalidDateRangeException("startDate must not be after endDate"));

        mockMvc.perform(get(BASE_PATH + "/1")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startDate must not be after endDate"));
    }

    @Test
    @DisplayName("returns 400 for a malformed date parameter")
    void returnsBadRequestForMalformedDate() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/1").param("startDate", "01-06-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("yyyy-MM-dd")));
    }

    @Test
    @DisplayName("returns 400 for a non numeric customer id")
    void returnsBadRequestForNonNumericCustomerId() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("returns 400 for a non positive customer id")
    void returnsBadRequestForNonPositiveCustomerId() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("returns a list when rewards are requested for every customer")
    void returnsRewardsForAllCustomers() throws Exception {
        when(rewardService.calculateRewardsForAllCustomers(any(), any()))
                .thenReturn(Arrays.asList(sampleResponse(), emptyResponse()));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].totalPoints").value(90))
                .andExpect(jsonPath("$[1].totalPoints").value(0));
    }

    @Test
    @DisplayName("serves the async endpoint through a dispatched deferred result")
    void returnsRewardsAsynchronously() throws Exception {
        when(asyncRewardService.calculateRewardsForCustomer(eq(1L), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(sampleResponse()));

        MvcResult asyncResult = mockMvc.perform(get(BASE_PATH + "/1/async"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalPoints").value(90));
    }

    private CustomerRewardResponse sampleResponse() {
        TransactionDetail transaction = new TransactionDetail(10L, LocalDate.of(2026, 6, 10),
                new BigDecimal("120.00"), 90, "Electronics");

        MonthlyRewardSummary june = new MonthlyRewardSummary("2026-06", "JUNE", 6, 2026, 1,
                new BigDecimal("120.00"), 90);

        RewardSummary summary = new RewardSummary(1, new BigDecimal("120.00"),
                new BigDecimal("120.00"), new BigDecimal("120.00"), 3, 1,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10));

        return new CustomerRewardResponse(1L, "Alice Johnson", "alice.johnson@example.com",
                LocalDate.of(2021, 3, 18), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 15),
                90, Collections.singletonList(june), summary,
                Collections.singletonList(transaction));
    }

    private CustomerRewardResponse emptyResponse() {
        RewardSummary summary = new RewardSummary(0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 3, 0, null, null);

        return new CustomerRewardResponse(2L, "Emily Watson", "emily.watson@example.com",
                LocalDate.of(2024, 5, 6), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 15),
                0, Collections.emptyList(), summary, Collections.emptyList());
    }
}
