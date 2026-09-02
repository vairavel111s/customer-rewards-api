package com.retailer.rewards;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end tests over the real application context and the seeded in-memory database.
 *
 * <p>The seed data is anchored to the current month, so the expectations below are derived
 * from {@link YearMonth#now()} rather than hard coded calendar dates. The point totals
 * themselves are fixed, because the amounts in the data set are fixed.</p>
 *
 * <p>Expected totals for the default three month window (current month and the two before
 * it), worked out by hand from the seed data:</p>
 * <pre>
 *   Alice   $120.00 = 90, $75.50 = 25, $45.00 = 0     -> 115
 *           $200.00 = 250, $99.99 = 49                -> 299
 *           $310.25 = 470, $50.00 = 0                 -> 470   total 884
 *   Brian   $60.00 = 10, $100.00 = 50, $100.01 = 50, $149.99 = 148  total 258
 *           (a $500.00 purchase four months ago is outside the window)
 *   Carla   $49.99/$50.00/$50.99 = 0, $51.00 = 1, $999.99 = 1848    total 1849
 *   Emily   no transactions                                          total 0
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsApiIntegrationTest {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String REWARDS_PATH = "/api/v1/rewards/customers";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("calculates the three month total and monthly breakdown for a customer")
    void calculatesRewardsOverTheDefaultWindow() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/1")
                        .param("startDate", windowStart())
                        .param("endDate", today()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.totalPoints").value(884))
                .andExpect(jsonPath("$.monthlyBreakdown.length()").value(3))
                .andExpect(jsonPath("$.monthlyBreakdown[0].pointsEarned").value(115))
                .andExpect(jsonPath("$.monthlyBreakdown[1].pointsEarned").value(299))
                .andExpect(jsonPath("$.monthlyBreakdown[2].pointsEarned").value(470))
                .andExpect(jsonPath("$.summary.totalTransactions").value(7))
                .andExpect(jsonPath("$.summary.monthsCovered").value(3))
                .andExpect(jsonPath("$.transactions.length()").value(7));
    }

    @Test
    @DisplayName("uses the default three month window when no dates are supplied")
    void appliesDefaultWindowWhenDatesOmitted() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value(windowStart()))
                .andExpect(jsonPath("$.periodEnd").value(today()))
                .andExpect(jsonPath("$.totalPoints").value(884));
    }

    @Test
    @DisplayName("scores the threshold edge cases exactly as specified")
    void scoresThresholdEdgeCases() throws Exception {
        // Carla's purchases sit either side of both thresholds: $49.99, $50.00 and $50.99
        // all earn nothing, $51.00 earns a single point.
        mockMvc.perform(get(REWARDS_PATH + "/3")
                        .param("startDate", windowStart())
                        .param("endDate", today()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Carla Mendes"))
                .andExpect(jsonPath("$.totalPoints").value(1849))
                .andExpect(jsonPath("$.monthlyBreakdown[0].pointsEarned").value(1))
                .andExpect(jsonPath("$.monthlyBreakdown[1].pointsEarned").value(0))
                .andExpect(jsonPath("$.monthlyBreakdown[1].transactionCount").value(0))
                .andExpect(jsonPath("$.monthlyBreakdown[2].pointsEarned").value(1848));
    }

    @Test
    @DisplayName("excludes transactions that fall outside the requested window")
    void excludesTransactionsOutsideTheWindow() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/2")
                        .param("startDate", windowStart())
                        .param("endDate", today()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoints").value(258))
                .andExpect(jsonPath("$.summary.totalTransactions").value(4));
    }

    @Test
    @DisplayName("includes the earlier transaction once the window is widened")
    void includesEarlierTransactionWhenWindowWidened() throws Exception {
        String widerStart = YearMonth.now().minusMonths(4).atDay(1).format(ISO_DATE);

        mockMvc.perform(get(REWARDS_PATH + "/2")
                        .param("startDate", widerStart)
                        .param("endDate", today()))
                .andExpect(status().isOk())
                // The $500.00 purchase adds a further 850 points.
                .andExpect(jsonPath("$.totalPoints").value(1108))
                .andExpect(jsonPath("$.summary.totalTransactions").value(5))
                .andExpect(jsonPath("$.summary.monthsCovered").value(5));
    }

    @Test
    @DisplayName("returns zero totals for a customer with no purchases")
    void returnsZeroTotalsForInactiveCustomer() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Emily Watson"))
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.transactions").isEmpty())
                .andExpect(jsonPath("$.monthlyBreakdown.length()").value(3))
                .andExpect(jsonPath("$.summary.totalAmountSpent").value(0));
    }

    @Test
    @DisplayName("reports on every customer in one call")
    void reportsOnEveryCustomer() throws Exception {
        mockMvc.perform(get(REWARDS_PATH)
                        .param("startDate", windowStart())
                        .param("endDate", today()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].totalPoints").value(884))
                .andExpect(jsonPath("$[1].totalPoints").value(258))
                .andExpect(jsonPath("$[2].totalPoints").value(1849))
                .andExpect(jsonPath("$[4].totalPoints").value(0));
    }

    @Test
    @DisplayName("serves the same figures through the asynchronous endpoint")
    void servesRewardsAsynchronously() throws Exception {
        MvcResult started = mockMvc.perform(get(REWARDS_PATH + "/1/async"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.totalPoints").value(884));
    }

    @Test
    @DisplayName("returns 404 for an unknown customer")
    void returnsNotFoundForUnknownCustomer() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No customer found with id 9999"));
    }

    @Test
    @DisplayName("returns 400 when the start date is after the end date")
    void returnsBadRequestForInvertedRange() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/1")
                        .param("startDate", today())
                        .param("endDate", LocalDate.now().minusMonths(1).format(ISO_DATE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("returns 400 for a date range wider than the configured maximum")
    void returnsBadRequestForOversizedRange() throws Exception {
        mockMvc.perform(get(REWARDS_PATH + "/1")
                        .param("startDate", LocalDate.now().minusYears(5).format(ISO_DATE))
                        .param("endDate", today()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("exceeds the maximum")));
    }

    @Test
    @DisplayName("lists the seeded customers")
    void listsSeededCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Alice Johnson"))
                .andExpect(jsonPath("$[4].name").value("Emily Watson"))
                .andExpect(jsonPath("$[4].totalTransactions").value(0));
    }

    @Test
    @DisplayName("pages a customer's transaction history newest first")
    void pagesTransactionHistory() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1/transactions")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));
    }

    private String today() {
        return LocalDate.now().format(ISO_DATE);
    }

    private String windowStart() {
        return YearMonth.now().minusMonths(2).atDay(1).format(ISO_DATE);
    }
}
