package com.retailer.rewards.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import com.retailer.rewards.config.RewardProperties;
import com.retailer.rewards.exception.InvalidDateRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns the optional {@code startDate} / {@code endDate} query parameters into a validated,
 * fully populated {@link DateRange}.
 *
 * <p>Both parameters are optional so the endpoint stays usable with no arguments at all.
 * When they are omitted the range falls back to the configured default window (three
 * calendar months, which is the scenario in the requirements), which keeps the API
 * dynamic without forcing every caller to compute dates.</p>
 *
 * <p>A {@link Clock} is injected rather than calling {@code LocalDate.now()} directly so
 * that "today" can be pinned in tests.</p>
 */
@Component
public class DateRangeResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateRangeResolver.class);

    private final RewardProperties properties;
    private final Clock clock;

    public DateRangeResolver(RewardProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Resolves and validates the requested period.
     *
     * @param requestedStart caller supplied start date, may be {@code null}
     * @param requestedEnd   caller supplied end date, may be {@code null}
     * @return an inclusive range that is guaranteed to be non null, ordered and bounded
     * @throws InvalidDateRangeException if the resulting range is not usable
     */
    public DateRange resolve(LocalDate requestedStart, LocalDate requestedEnd) {
        LocalDate today = LocalDate.now(clock);

        LocalDate endDate = (requestedEnd != null) ? requestedEnd : today;
        LocalDate startDate = (requestedStart != null) ? requestedStart : defaultStartFor(endDate);

        validate(startDate, endDate, today);

        DateRange range = new DateRange(startDate, endDate);
        LOGGER.debug("Resolved reward period {} (requestedStart={}, requestedEnd={})",
                range, requestedStart, requestedEnd);
        return range;
    }

    /**
     * First day of the month that begins the default window ending in {@code endDate}'s
     * month. With a three month default and an end date in September, this is 1 July.
     */
    private LocalDate defaultStartFor(LocalDate endDate) {
        return YearMonth.from(endDate)
                .minusMonths(properties.getDefaultPeriodMonths() - 1L)
                .atDay(1);
    }

    private void validate(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException(
                    "startDate (" + startDate + ") must not be after endDate (" + endDate + ")");
        }
        if (startDate.isAfter(today)) {
            throw new InvalidDateRangeException("startDate (" + startDate + ") must not be in the future");
        }
        if (endDate.isAfter(today)) {
            throw new InvalidDateRangeException("endDate (" + endDate + ") must not be in the future");
        }

        int monthsCovered = new DateRange(startDate, endDate).monthsCovered();
        if (monthsCovered > properties.getMaxPeriodMonths()) {
            throw new InvalidDateRangeException("Requested period spans " + monthsCovered
                    + " months, which exceeds the maximum of " + properties.getMaxPeriodMonths());
        }
    }
}
