package com.retailer.rewards.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.retailer.rewards.entity.Customer;
import com.retailer.rewards.entity.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the demonstration data set into the in-memory database at start up.
 *
 * <p>The data is anchored to the current month rather than to fixed calendar dates, so the
 * default three month window always has something to show no matter when the application
 * is run. It is chosen to exercise every interesting branch of the reward rule:</p>
 *
 * <ul>
 *   <li>amounts either side of both thresholds ($49.99, $50.00, $50.99, $51.00, $100.00, $100.01);</li>
 *   <li>the worked example from the requirements ($120.00 = 90 points);</li>
 *   <li>a large purchase, a customer with a month of no activity, a customer with no
 *       transactions at all, and a purchase deliberately placed outside the default window
 *       to prove the date filter works.</li>
 * </ul>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSeeder.class);

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public DataSeeder(CustomerRepository customerRepository,
                      TransactionRepository transactionRepository,
                      Clock clock) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            LOGGER.info("Customer data already present, skipping seed");
            return;
        }

        YearMonth currentMonth = YearMonth.now(clock);
        YearMonth oneMonthAgo = currentMonth.minusMonths(1);
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);
        YearMonth fourMonthsAgo = currentMonth.minusMonths(4);

        Customer alice = customerRepository.save(
                new Customer("Alice Johnson", "alice.johnson@example.com", LocalDate.of(2021, 3, 18)));
        Customer brian = customerRepository.save(
                new Customer("Brian Chen", "brian.chen@example.com", LocalDate.of(2019, 11, 2)));
        Customer carla = customerRepository.save(
                new Customer("Carla Mendes", "carla.mendes@example.com", LocalDate.of(2022, 7, 9)));
        Customer daniel = customerRepository.save(
                new Customer("Daniel O'Neill", "daniel.oneill@example.com", LocalDate.of(2020, 1, 24)));
        Customer emily = customerRepository.save(
                new Customer("Emily Watson", "emily.watson@example.com", LocalDate.of(2024, 5, 6)));
        Customer frank = customerRepository.save(
                new Customer("Frank Miller", "frank.miller@example.com", LocalDate.of(2023, 9, 30)));

        List<Transaction> transactions = new ArrayList<>();

        // Alice: steady spender across all three months of the default window.
        transactions.add(purchase(alice, "120.00", twoMonthsAgo, 6, "Electronics"));
        transactions.add(purchase(alice, "75.50", twoMonthsAgo, 17, "Groceries"));
        transactions.add(purchase(alice, "45.00", twoMonthsAgo, 25, "Books"));
        transactions.add(purchase(alice, "200.00", oneMonthAgo, 4, "Home appliance"));
        transactions.add(purchase(alice, "99.99", oneMonthAgo, 21, "Clothing"));
        transactions.add(purchase(alice, "310.25", currentMonth, 3, "Furniture"));
        transactions.add(purchase(alice, "50.00", currentMonth, 12, "Pharmacy"));

        // Brian: one purchase sits outside the default window and must be excluded from it.
        transactions.add(purchase(brian, "500.00", fourMonthsAgo, 14, "Travel booking"));
        transactions.add(purchase(brian, "60.00", twoMonthsAgo, 9, "Groceries"));
        transactions.add(purchase(brian, "100.00", oneMonthAgo, 2, "Fuel"));
        transactions.add(purchase(brian, "100.01", oneMonthAgo, 19, "Sporting goods"));
        transactions.add(purchase(brian, "149.99", currentMonth, 8, "Headphones"));

        // Carla: threshold edge cases, then a quiet month, then a large purchase.
        transactions.add(purchase(carla, "49.99", twoMonthsAgo, 3, "Stationery"));
        transactions.add(purchase(carla, "50.00", twoMonthsAgo, 11, "Toys"));
        transactions.add(purchase(carla, "50.99", twoMonthsAgo, 18, "Garden supplies"));
        transactions.add(purchase(carla, "51.00", twoMonthsAgo, 27, "Kitchenware"));
        transactions.add(purchase(carla, "999.99", currentMonth, 5, "Laptop"));

        // Daniel: high value purchases plus one that earns nothing.
        transactions.add(purchase(daniel, "1000.00", twoMonthsAgo, 22, "Television"));
        transactions.add(purchase(daniel, "250.75", oneMonthAgo, 13, "Power tools"));
        transactions.add(purchase(daniel, "10.00", currentMonth, 2, "Coffee"));

        // Emily has no transactions at all and must still appear with zero totals.

        // Frank joined recently and only has activity in the current month.
        transactions.add(purchase(frank, "130.00", currentMonth, 7, "Footwear"));
        transactions.add(purchase(frank, "55.00", currentMonth, 15, "Groceries"));

        transactionRepository.saveAll(transactions);

        LOGGER.info("Seeded {} customers and {} transactions (including {} with no activity)",
                customerRepository.count(), transactions.size(), emily.getName());
    }

    private Transaction purchase(Customer customer, String amount, YearMonth month, int dayOfMonth,
                                 String description) {
        return new Transaction(customer, new BigDecimal(amount), dateWithin(month, dayOfMonth),
                description);
    }

    /**
     * Places a transaction on the requested day, clamped so that it never lands after the
     * end of the month or in the future. Without the clamp, seeding early in a month would
     * push transactions past today and they would fall outside every default query.
     */
    private LocalDate dateWithin(YearMonth month, int dayOfMonth) {
        LocalDate today = LocalDate.now(clock);
        LocalDate candidate = month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
        return candidate.isAfter(today) ? today : candidate;
    }
}
