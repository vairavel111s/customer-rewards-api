package com.retailer.rewards.repository;

import java.time.LocalDate;
import java.util.List;

import com.retailer.rewards.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data access for {@link Transaction} records.
 *
 * <p>The date range queries are inclusive on both ends, matching the semantics documented
 * on the REST endpoints.</p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Transactions for one customer inside an inclusive date range, oldest first.
     */
    List<Transaction> findByCustomerIdAndTransactionDateBetweenOrderByTransactionDateAsc(
            Long customerId, LocalDate startDate, LocalDate endDate);

    /**
     * Transactions for every customer inside an inclusive date range.
     *
     * <p>The customer is fetched eagerly in the same query to avoid the N+1 select that a
     * lazy association would otherwise trigger when the response is assembled.</p>
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.customer c "
            + "WHERE t.transactionDate BETWEEN :startDate AND :endDate "
            + "ORDER BY c.id ASC, t.transactionDate ASC")
    List<Transaction> findAllInPeriodWithCustomer(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    Page<Transaction> findByCustomerIdOrderByTransactionDateDesc(Long customerId,
                                                                 Pageable pageable);

    long countByCustomerId(Long customerId);
}
