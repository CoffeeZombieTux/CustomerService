package io.customerservice.customerservice.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient bonus balance for this redemption");
    }
}