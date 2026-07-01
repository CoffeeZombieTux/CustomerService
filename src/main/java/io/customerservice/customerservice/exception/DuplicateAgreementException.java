package io.customerservice.customerservice.exception;

public class DuplicateAgreementException extends RuntimeException {
    public DuplicateAgreementException() {
        super("This agreement is already recorded with the same version and status");
    }
}
