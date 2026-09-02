package com.retailer.rewards.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * An immutable, inclusive date range used to scope a reward calculation.
 */
public final class DateRange {

    private final LocalDate startDate;
    private final LocalDate endDate;

    public DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Number of calendar months the range touches, counting partial months at either end.
     */
    public int monthsCovered() {
        long months = ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(endDate));
        return (int) months + 1;
    }

    @Override
    public String toString() {
        return startDate + " to " + endDate;
    }
}
