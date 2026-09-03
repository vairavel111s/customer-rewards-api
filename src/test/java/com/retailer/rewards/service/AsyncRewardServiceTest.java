package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.exception.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the asynchronous wrapper.
 *
 * <p>The delegate is mocked: what matters here is that the wrapper hands the call
 * straight through and surfaces failures on the future, not that the arithmetic is right.
 * The arithmetic is covered by {@link RewardServiceImplTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
class AsyncRewardServiceTest {

    @Mock
    private RewardService rewardService;

    private AsyncRewardService asyncRewardService;

    @BeforeEach
    void setUp() {
        RewardProperties properties = new RewardProperties();
        // Drop the artificial delay so the tests do not pay for it.
        properties.setAsyncSimulatedLatencyMs(0L);

        asyncRewardService = new AsyncRewardService(rewardService, properties);
    }

    @Test
    @DisplayName("returns the delegate's payload on the future")
    void returnsDelegateResult() throws Exception {
        when(rewardService.calculateRewardsForCustomer(1L, null, null))
                .thenReturn(sampleResponse());

        CustomerRewardResponse response =
                asyncRewardService.calculateRewardsForCustomer(1L, null, null).get();

        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getTotalPoints()).isEqualTo(90);
    }

    @Test
    @DisplayName("passes the requested period through unchanged")
    void forwardsRequestedPeriod() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        when(rewardService.calculateRewardsForCustomer(1L, start, end))
                .thenReturn(sampleResponse());

        asyncRewardService.calculateRewardsForCustomer(1L, start, end);

        verify(rewardService).calculateRewardsForCustomer(1L, start, end);
    }

    @Test
    @DisplayName("surfaces a delegate failure rather than swallowing it")
    void propagatesDelegateFailure() {
        when(rewardService.calculateRewardsForCustomer(99L, null, null))
                .thenThrow(new CustomerNotFoundException(99L));

        assertThatThrownBy(() -> asyncRewardService.calculateRewardsForCustomer(99L, null, null))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    private CustomerRewardResponse sampleResponse() {
        RewardSummary summary = new RewardSummary(1, new BigDecimal("120.00"),
                new BigDecimal("120.00"), new BigDecimal("120.00"), 3, 1,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10));

        return new CustomerRewardResponse(1L, "Alice Johnson", "alice.johnson@example.com",
                LocalDate.of(2021, 3, 18), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 15),
                90, Collections.emptyList(), summary, Collections.emptyList());
    }
}
