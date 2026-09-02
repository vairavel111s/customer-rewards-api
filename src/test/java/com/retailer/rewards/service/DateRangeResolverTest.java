package com.retailer.rewards.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.exception.InvalidDateRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the date range defaulting and validation rules.
 *
 * <p>"Today" is pinned to 15 August 2026 through a fixed {@link Clock} so the expectations
 * do not drift with the calendar.</p>
 */
class DateRangeResolverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 15);

    private RewardProperties properties;
    private DateRangeResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RewardProperties();
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneId.of("UTC"));
        resolver = new DateRangeResolver(properties, fixedClock);
    }

    @Test
    @DisplayName("defaults to the three calendar month window ending today")
    void defaultsToThreeMonthWindow() {
        DateRange range = resolver.resolve(null, null);

        assertThat(range.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(range.getEndDate()).isEqualTo(TODAY);
        assertThat(range.monthsCovered()).isEqualTo(3);
    }

    @Test
    @DisplayName("defaults the end date to today when only a start date is supplied")
    void defaultsEndDateToToday() {
        DateRange range = resolver.resolve(LocalDate.of(2026, 1, 10), null);

        assertThat(range.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(range.getEndDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("derives the start date from the supplied end date")
    void derivesStartDateFromEndDate() {
        DateRange range = resolver.resolve(null, LocalDate.of(2026, 5, 20));

        assertThat(range.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(range.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("uses both dates unchanged when the caller supplies them")
    void honoursExplicitRange() {
        DateRange range = resolver.resolve(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30));

        assertThat(range.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(range.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(range.monthsCovered()).isEqualTo(3);
    }

    @Test
    @DisplayName("accepts a single day range")
    void acceptsSingleDayRange() {
        DateRange range = resolver.resolve(TODAY, TODAY);

        assertThat(range.monthsCovered()).isEqualTo(1);
    }

    @Test
    @DisplayName("respects a reconfigured default window")
    void respectsConfiguredDefaultWindow() {
        properties.setDefaultPeriodMonths(6);

        DateRange range = resolver.resolve(null, null);

        assertThat(range.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("rejects a start date after the end date")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> resolver.resolve(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("must not be after endDate");
    }

    @Test
    @DisplayName("rejects an end date in the future")
    void rejectsFutureEndDate() {
        assertThatThrownBy(() -> resolver.resolve(null, TODAY.plusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    @DisplayName("rejects a start date in the future")
    void rejectsFutureStartDate() {
        assertThatThrownBy(() -> resolver.resolve(TODAY.plusDays(5), null))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("must not be after endDate");
    }

    @Test
    @DisplayName("rejects a range wider than the configured maximum")
    void rejectsOversizedRange() {
        assertThatThrownBy(() -> resolver.resolve(LocalDate.of(2020, 1, 1), TODAY))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("exceeds the maximum");
    }
}
