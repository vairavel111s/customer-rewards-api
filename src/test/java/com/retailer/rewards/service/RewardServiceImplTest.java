package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.dto.MonthlyRewardSummary;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the aggregation layer.
 *
 * <p>Only the repositories are mocked. The calculator and the date range resolver are real
 * collaborators, because the value of these tests lies in checking that transactions are
 * grouped and totalled correctly, not in restating the arithmetic a mock would return.</p>
 */
@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private RewardServiceImpl rewardService;

    private Customer alice;

    @BeforeEach
    void setUp() {
        RewardProperties properties = new RewardProperties();
        // Remove the artificial delay so the async test does not pay for it.
        properties.setAsyncSimulatedLatencyMs(0L);

        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneId.of("UTC"));

        rewardService = new RewardServiceImpl(
                customerRepository,
                transactionRepository,
                new RewardCalculator(properties),
                new DateRangeResolver(properties, fixedClock),
                properties);

        alice = customer(1L, "Alice Johnson", "alice.johnson@example.com");
    }

    @Test
    @DisplayName("totals points per month and overall for a customer with activity")
    void calculatesMonthlyAndTotalPoints() {
        List<Transaction> transactions = Arrays.asList(
                transaction(10L, alice, "120.00", LocalDate.of(2026, 6, 10), "Electronics"),
                transaction(11L, alice, "200.00", LocalDate.of(2026, 7, 5), "Home appliance"),
                transaction(12L, alice, "60.00", LocalDate.of(2026, 7, 22), "Groceries"),
                transaction(13L, alice, "1000.00", LocalDate.of(2026, 8, 1), "Television"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        1L, PERIOD_START, TODAY))
                .thenReturn(transactions);

        CustomerRewardResponse response = rewardService.calculateRewardsForCustomer(1L, null, null);

        assertThat(response.getCustomerId()).isEqualTo(1L);
        assertThat(response.getCustomerName()).isEqualTo("Alice Johnson");
        assertThat(response.getPeriodStart()).isEqualTo(PERIOD_START);
        assertThat(response.getPeriodEnd()).isEqualTo(TODAY);

        // 90 in June, 250 + 10 in July, 1850 in August.
        assertThat(response.getTotalPoints()).isEqualTo(2200);

        assertThat(response.getMonthlyBreakdown())
                .extracting(MonthlyRewardSummary::getMonth, MonthlyRewardSummary::getPointsEarned,
                        MonthlyRewardSummary::getTransactionCount)
                .containsExactly(
                        tuple("JUNE", 90, 1),
                        tuple("JULY", 260, 2),
                        tuple("AUGUST", 1850, 1));

        assertThat(response.getTransactions()).hasSize(4);
        assertThat(response.getTransactions().get(0).getPointsEarned()).isEqualTo(90);
    }

    @Test
    @DisplayName("reports the spending summary alongside the points")
    void populatesSpendingSummary() {
        List<Transaction> transactions = Arrays.asList(
                transaction(10L, alice, "120.00", LocalDate.of(2026, 6, 10), "Electronics"),
                transaction(11L, alice, "200.00", LocalDate.of(2026, 7, 5), "Home appliance"),
                transaction(12L, alice, "60.00", LocalDate.of(2026, 7, 22), "Groceries"),
                transaction(13L, alice, "1000.00", LocalDate.of(2026, 8, 1), "Television"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        anyLong(), any(), any()))
                .thenReturn(transactions);

        CustomerRewardResponse response = rewardService.calculateRewardsForCustomer(1L, null, null);

        assertThat(response.getSummary().getTotalTransactions()).isEqualTo(4);
        assertThat(response.getSummary().getTotalAmountSpent())
                .isEqualByComparingTo(new BigDecimal("1380.00"));
        assertThat(response.getSummary().getAverageTransactionAmount())
                .isEqualByComparingTo(new BigDecimal("345.00"));
        assertThat(response.getSummary().getHighestTransactionAmount())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getSummary().getMonthsCovered()).isEqualTo(3);
        assertThat(response.getSummary().getMonthsWithActivity()).isEqualTo(3);
        assertThat(response.getSummary().getFirstTransactionDate())
                .isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.getSummary().getLastTransactionDate())
                .isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("still lists every month in the period when some have no activity")
    void includesMonthsWithoutActivity() {
        List<Transaction> transactions = Collections.singletonList(
                transaction(20L, alice, "120.00", LocalDate.of(2026, 6, 10), "Electronics"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        anyLong(), any(), any()))
                .thenReturn(transactions);

        CustomerRewardResponse response = rewardService.calculateRewardsForCustomer(1L, null, null);

        assertThat(response.getMonthlyBreakdown()).hasSize(3);
        assertThat(response.getMonthlyBreakdown().get(1).getMonth()).isEqualTo("JULY");
        assertThat(response.getMonthlyBreakdown().get(1).getPointsEarned()).isZero();
        assertThat(response.getMonthlyBreakdown().get(1).getTransactionCount()).isZero();
        assertThat(response.getMonthlyBreakdown().get(1).getAmountSpent())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getSummary().getMonthsWithActivity()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns zero totals for a customer with no transactions in the period")
    void returnsZeroTotalsWhenNoTransactions() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        CustomerRewardResponse response = rewardService.calculateRewardsForCustomer(1L, null, null);

        assertThat(response.getTotalPoints()).isZero();
        assertThat(response.getTransactions()).isEmpty();
        assertThat(response.getMonthlyBreakdown()).hasSize(3);
        assertThat(response.getSummary().getTotalAmountSpent())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getSummary().getAverageTransactionAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getSummary().getFirstTransactionDate()).isNull();
    }

    @Test
    @DisplayName("fails with a not found error for an unknown customer")
    void throwsWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.calculateRewardsForCustomer(99L, null, null))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("rejects an invalid date range before touching the database")
    void validatesDateRangeBeforeQuerying() {
        assertThatThrownBy(() -> rewardService.calculateRewardsForCustomer(
                1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(InvalidDateRangeException.class);

        verify(customerRepository, never()).findById(anyLong());
        verify(transactionRepository, never())
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        anyLong(), any(), any());
    }

    @Test
    @DisplayName("honours an explicitly requested period")
    void honoursExplicitPeriod() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(1L, start, end))
                .thenReturn(Collections.singletonList(
                        transaction(30L, alice, "120.00", LocalDate.of(2026, 7, 10), "Electronics")));

        CustomerRewardResponse response = rewardService.calculateRewardsForCustomer(1L, start, end);

        assertThat(response.getPeriodStart()).isEqualTo(start);
        assertThat(response.getPeriodEnd()).isEqualTo(end);
        assertThat(response.getMonthlyBreakdown()).hasSize(1);
        assertThat(response.getTotalPoints()).isEqualTo(90);
    }

    @Test
    @DisplayName("includes customers with no activity when reporting on everyone")
    void includesInactiveCustomersInBulkReport() {
        Customer emily = customer(2L, "Emily Watson", "emily.watson@example.com");

        when(customerRepository.findAll()).thenReturn(Arrays.asList(alice, emily));
        when(transactionRepository.findAllInPeriodWithCustomer(PERIOD_START, TODAY))
                .thenReturn(Collections.singletonList(
                        transaction(40L, alice, "120.00", LocalDate.of(2026, 6, 10), "Electronics")));

        List<CustomerRewardResponse> responses =
                rewardService.calculateRewardsForAllCustomers(null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getCustomerName()).isEqualTo("Alice Johnson");
        assertThat(responses.get(0).getTotalPoints()).isEqualTo(90);
        assertThat(responses.get(1).getCustomerName()).isEqualTo("Emily Watson");
        assertThat(responses.get(1).getTotalPoints()).isZero();
        assertThat(responses.get(1).getTransactions()).isEmpty();
    }

    @Test
    @DisplayName("produces the same payload through the asynchronous path")
    void asyncPathReturnsSameResult() throws Exception {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(
                        transaction(50L, alice, "120.00", LocalDate.of(2026, 6, 10), "Electronics")));

        CustomerRewardResponse response =
                rewardService.calculateRewardsForCustomerAsync(1L, null, null).get();

        assertThat(response.getTotalPoints()).isEqualTo(90);
        assertThat(response.getCustomerId()).isEqualTo(1L);
    }

    private Customer customer(Long id, String name, String email) {
        Customer created = new Customer(name, email, LocalDate.of(2021, 3, 18));
        created.setId(id);
        return created;
    }

    private Transaction transaction(Long id, Customer owner, String amount, LocalDate date,
                                    String description) {
        Transaction created = new Transaction(owner, new BigDecimal(amount), date, description);
        created.setId(id);
        return created;
    }
}
