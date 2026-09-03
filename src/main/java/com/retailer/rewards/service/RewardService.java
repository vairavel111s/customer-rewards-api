package com.retailer.rewards.service;

import java.time.LocalDate;
import java.util.List;

import com.retailer.rewards.dto.CustomerRewardResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;

/**
 * Reward point calculations over a customer's recorded purchases.
 *
 * <p>The non blocking variant lives in {@link AsyncRewardService}, which delegates here.</p>
 */
public interface RewardService {

    /**
     * Calculates the reward points earned by one customer, per month and in total.
     *
     * @param customerId     the customer to report on
     * @param requestedStart inclusive start of the period, {@code null} for the default window
     * @param requestedEnd   inclusive end of the period, {@code null} for today
     * @return the fully populated reward response
     * @throws CustomerNotFoundException if the customer does not exist
     * @throws InvalidDateRangeException if the requested period is not usable
     */
    CustomerRewardResponse calculateRewardsForCustomer(Long customerId, LocalDate requestedStart,
                                                       LocalDate requestedEnd);

    /**
     * Calculates reward points for every customer over the same period. Customers with no
     * activity in the period are included with zero totals.
     */
    List<CustomerRewardResponse> calculateRewardsForAllCustomers(LocalDate requestedStart,
                                                                 LocalDate requestedEnd);
}
