package io.customerservice.customerservice.exception;

public class MandatoryAgreementException extends RuntimeException {
    public MandatoryAgreementException() {
        super("All mandatory agreements must be accepted to register.");
    }
}
