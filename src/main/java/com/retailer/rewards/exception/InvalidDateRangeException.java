package com.retailer.rewards.exception;

/**
 * Raised when the caller supplied date range cannot be honoured, for example when the
 * start date falls after the end date or the range is wider than the configured maximum.
 * Surfaced to the caller as HTTP 400.
 */
public class InvalidDateRangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
