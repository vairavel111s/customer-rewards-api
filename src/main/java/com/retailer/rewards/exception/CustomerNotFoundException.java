package com.retailer.rewards.exception;

/**
 * Raised when a reward calculation is requested for a customer id that does not exist.
 * Surfaced to the caller as HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CustomerNotFoundException(Long customerId) {
        super("No customer found with id " + customerId);
    }
}
