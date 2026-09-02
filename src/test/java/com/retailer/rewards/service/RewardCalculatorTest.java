package com.retailer.rewards.service;

import java.math.BigDecimal;

import com.retailer.rewards.config.RewardProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the reward rule itself. This is the piece of logic the whole service
 * exists to apply, so the boundaries either side of both thresholds are covered explicitly.
 */
class RewardCalculatorTest {

    private RewardCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RewardCalculator(new RewardProperties());
    }

    @ParameterizedTest(name = "${0} earns {1} points")
    @CsvSource({
            // Below the lower threshold: nothing is earned.
            "0.00,      0",
            "10.00,     0",
            "49.99,     0",
            "50.00,     0",
            // Partial dollars are truncated, so 50.99 is still treated as 50.
            "50.99,     0",
            // First dollar that earns anything.
            "51.00,     1",
            "75.50,     25",
            "99.99,     49",
            // At the upper threshold only the lower tier has been earned.
            "100.00,    50",
            "100.01,    50",
            // The worked example from the requirements: 2 x $20 + 1 x $50.
            "120.00,    90",
            "130.00,    110",
            "149.99,    148",
            "200.00,    250",
            "250.75,    350",
            "310.25,    470",
            "500.00,    850",
            "999.99,    1848",
            "1000.00,   1850"
    })
    @DisplayName("applies the tiered rule to a transaction amount")
    void calculatesPointsForAmount(BigDecimal amount, int expectedPoints) {
        assertThat(calculator.calculatePoints(amount)).isEqualTo(expectedPoints);
    }

    @Test
    @DisplayName("treats a null amount as zero rather than failing")
    void returnsZeroForNullAmount() {
        assertThat(calculator.calculatePoints(null)).isZero();
    }

    @ParameterizedTest(name = "refund of ${0} earns no points")
    @ValueSource(strings = {"-0.01", "-50.00", "-250.00"})
    @DisplayName("never awards negative points for a refund")
    void returnsZeroForNegativeAmount(String amount) {
        assertThat(calculator.calculatePoints(new BigDecimal(amount))).isZero();
    }

    @Test
    @DisplayName("rejects an amount large enough to overflow the point total")
    void rejectsAmountBeyondSupportedRange() {
        BigDecimal absurdAmount = new BigDecimal("1000000001");

        assertThatThrownBy(() -> calculator.calculatePoints(absurdAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the supported maximum");
    }

    @Nested
    @DisplayName("with reconfigured thresholds")
    class WithCustomThresholds {

        @Test
        @DisplayName("honours thresholds and rates supplied by configuration")
        void usesConfiguredThresholdsAndRates() {
            RewardProperties customProperties = new RewardProperties();
            customProperties.setLowerThreshold(20);
            customProperties.setUpperThreshold(40);
            customProperties.setLowerTierPointsPerDollar(2);
            customProperties.setUpperTierPointsPerDollar(5);

            RewardCalculator customCalculator = new RewardCalculator(customProperties);

            // $60 = 5 x $20 above the upper threshold + 2 x $20 between the thresholds.
            assertThat(customCalculator.calculatePoints(new BigDecimal("60.00"))).isEqualTo(140);
        }
    }
}
