package io.customerservice.customerservice.exception;

public class DuplicateIdempotencyKeyException extends RuntimeException {
    public DuplicateIdempotencyKeyException() {
        super("A transaction with this idempotency key was already processed");
    }
}
