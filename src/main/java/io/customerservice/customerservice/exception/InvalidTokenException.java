package io.customerservice.customerservice.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is invalid or has expired");
    }
}