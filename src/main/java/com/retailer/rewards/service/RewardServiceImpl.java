package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.retailer.rewards.config.AsyncConfig;
import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.dto.MonthlyRewardSummary;
import com.retailer.rewards.dto.RewardSummary;
import com.retailer.rewards.dto.TransactionDetail;
import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link RewardService} implementation.
 *
 * <p>The flow is deliberately simple: resolve the period, load the matching transactions,
 * score each one through {@link RewardCalculator}, then fold the scores into a monthly
 * breakdown and a set of totals. All the arithmetic happens on {@link BigDecimal} so cent
 * amounts stay exact.</p>
 */
@Service
public class RewardServiceImpl implements RewardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RewardServiceImpl.class);

    private static final int MONEY_SCALE = 2;

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RewardCalculator rewardCalculator;
    private final DateRangeResolver dateRangeResolver;
    private final RewardProperties properties;

    public RewardServiceImpl(CustomerRepository customerRepository,
                             TransactionRepository transactionRepository,
                             RewardCalculator rewardCalculator,
                             DateRangeResolver dateRangeResolver,
                             RewardProperties properties) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.rewardCalculator = rewardCalculator;
        this.dateRangeResolver = dateRangeResolver;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerRewardResponse calculateRewardsForCustomer(Long customerId,
                                                              LocalDate requestedStart,
                                                              LocalDate requestedEnd) {
        DateRange period = dateRangeResolver.resolve(requestedStart, requestedEnd);
        LOGGER.info("Calculating rewards for customer {} over {}", customerId, period);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                        customerId, period.getStartDate(), period.getEndDate());

        CustomerRewardResponse response = buildResponse(customer, transactions, period);
        LOGGER.info("Customer {} earned {} points from {} transactions over {}",
                customerId, response.getTotalPoints(), transactions.size(), period);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRewardResponse> calculateRewardsForAllCustomers(LocalDate requestedStart,
                                                                        LocalDate requestedEnd) {
        DateRange period = dateRangeResolver.resolve(requestedStart, requestedEnd);
        LOGGER.info("Calculating rewards for all customers over {}", period);

        List<Customer> customers = customerRepository.findAll();

        // One query for every transaction in the period, then group in memory. This keeps
        // the database round trips at two regardless of how many customers exist.
        Map<Long, List<Transaction>> transactionsByCustomer = transactionRepository
                .findAllInPeriodWithCustomer(period.getStartDate(), period.getEndDate())
                .stream()
                .collect(Collectors.groupingBy(transaction -> transaction.getCustomer().getId()));

        List<CustomerRewardResponse> responses = new ArrayList<>(customers.size());
        for (Customer customer : customers) {
            List<Transaction> transactions =
                    transactionsByCustomer.getOrDefault(customer.getId(), Collections.emptyList());
            responses.add(buildResponse(customer, transactions, period));
        }

        LOGGER.info("Calculated rewards for {} customers over {}", responses.size(), period);
        return responses;
    }

    @Override
    @Async(AsyncConfig.REWARDS_EXECUTOR)
    public CompletableFuture<CustomerRewardResponse> calculateRewardsForCustomerAsync(
            Long customerId, LocalDate requestedStart, LocalDate requestedEnd) {
        LOGGER.info("Async reward request for customer {} accepted on thread {}",
                customerId, Thread.currentThread().getName());

        simulateDownstreamLatency();

        CustomerRewardResponse response =
                calculateRewardsForCustomer(customerId, requestedStart, requestedEnd);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Stands in for a slow remote data source so the async path is observable end to end.
     */
    private void simulateDownstreamLatency() {
        long latencyMs = properties.getAsyncSimulatedLatencyMs();
        if (latencyMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Simulated latency interrupted, continuing without the delay");
        }
    }

    /**
     * Assembles the response payload from a customer and their transactions in the period.
     */
    private CustomerRewardResponse buildResponse(Customer customer, List<Transaction> transactions,
                                                 DateRange period) {
        List<TransactionDetail> details = toTransactionDetails(transactions);
        List<MonthlyRewardSummary> monthlyBreakdown = buildMonthlyBreakdown(transactions, period);

        int totalPoints = details.stream()
                .mapToInt(TransactionDetail::getPointsEarned)
                .sum();

        return new CustomerRewardResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getMemberSince(),
                period.getStartDate(),
                period.getEndDate(),
                totalPoints,
                monthlyBreakdown,
                buildSummary(transactions, details, monthlyBreakdown, period),
                details);
    }

    private List<TransactionDetail> toTransactionDetails(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> new TransactionDetail(
                        transaction.getId(),
                        transaction.getTransactionDate(),
                        scaleMoney(transaction.getAmount()),
                        rewardCalculator.calculatePoints(transaction.getAmount()),
                        transaction.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Produces one entry per calendar month in the period, including months with no
     * activity, so the caller receives a gap free timeline.
     */
    private List<MonthlyRewardSummary> buildMonthlyBreakdown(List<Transaction> transactions,
                                                             DateRange period) {
        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> YearMonth.from(transaction.getTransactionDate()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        YearMonth firstMonth = YearMonth.from(period.getStartDate());
        YearMonth lastMonth = YearMonth.from(period.getEndDate());

        List<MonthlyRewardSummary> breakdown = new ArrayList<>(period.monthsCovered());
        for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            List<Transaction> monthTransactions = byMonth.getOrDefault(month, Collections.emptyList());

            int monthPoints = monthTransactions.stream()
                    .mapToInt(transaction -> rewardCalculator.calculatePoints(transaction.getAmount()))
                    .sum();

            breakdown.add(new MonthlyRewardSummary(
                    month.toString(),
                    month.getMonth().name(),
                    month.getMonthValue(),
                    month.getYear(),
                    monthTransactions.size(),
                    scaleMoney(sumAmounts(monthTransactions)),
                    monthPoints));
        }
        return breakdown;
    }

    private RewardSummary buildSummary(List<Transaction> transactions,
                                       List<TransactionDetail> details,
                                       List<MonthlyRewardSummary> monthlyBreakdown,
                                       DateRange period) {
        int transactionCount = transactions.size();
        BigDecimal totalSpent = sumAmounts(transactions);

        BigDecimal averageAmount = (transactionCount == 0)
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : totalSpent.divide(BigDecimal.valueOf(transactionCount), MONEY_SCALE,
                        RoundingMode.HALF_UP);

        BigDecimal highestAmount = transactions.stream()
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        int monthsWithActivity = (int) monthlyBreakdown.stream()
                .filter(month -> month.getTransactionCount() > 0)
                .count();

        LocalDate firstDate = details.isEmpty() ? null : details.get(0).getTransactionDate();
        LocalDate lastDate = details.isEmpty() ? null
                : details.get(details.size() - 1).getTransactionDate();

        return new RewardSummary(
                transactionCount,
                scaleMoney(totalSpent),
                averageAmount,
                scaleMoney(highestAmount),
                period.monthsCovered(),
                monthsWithActivity,
                firstDate,
                lastDate);
    }

    private BigDecimal sumAmounts(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
