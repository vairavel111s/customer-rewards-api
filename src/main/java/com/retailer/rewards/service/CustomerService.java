package com.retailer.rewards.service;

import java.util.List;
import java.util.stream.Collectors;

import com.retailer.rewards.dto.CustomerResponse;
import com.retailer.rewards.dto.PagedResponse;
import com.retailer.rewards.dto.TransactionDetail;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read operations over customers and their raw transaction history.
 *
 * <p>These endpoints are supporting cast for the reward calculation: they let a caller
 * discover which customer ids exist and drill into the underlying purchases.</p>
 */
@Service
public class CustomerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RewardCalculator rewardCalculator;

    public CustomerService(CustomerRepository customerRepository,
                           TransactionRepository transactionRepository,
                           RewardCalculator rewardCalculator) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.rewardCalculator = rewardCalculator;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAllCustomers() {
        List<CustomerResponse> customers = customerRepository.findAll().stream()
                .map(customer -> new CustomerResponse(
                        customer.getId(),
                        customer.getName(),
                        customer.getEmail(),
                        customer.getMemberSince(),
                        transactionRepository.countByCustomerId(customer.getId())))
                .collect(Collectors.toList());

        LOGGER.debug("Returning {} customers", customers.size());
        return customers;
    }

    /**
     * Returns a page of the customer's transactions, newest first.
     *
     * @throws CustomerNotFoundException if the customer does not exist
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionDetail> findTransactions(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        Page<Transaction> page =
                transactionRepository.findByCustomerIdOrderByTransactionDateDesc(customerId, pageable);

        List<TransactionDetail> content = page.getContent().stream()
                .map(transaction -> new TransactionDetail(
                        transaction.getId(),
                        transaction.getTransactionDate(),
                        transaction.getAmount(),
                        rewardCalculator.calculatePoints(transaction.getAmount()),
                        transaction.getDescription()))
                .collect(Collectors.toList());

        LOGGER.debug("Returning page {} of transactions for customer {} ({} total)",
                page.getNumber(), customerId, page.getTotalElements());
        return PagedResponse.from(page, content);
    }
}
