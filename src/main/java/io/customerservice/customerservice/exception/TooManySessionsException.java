package io.customerservice.customerservice.exception;

public class TooManySessionsException extends RuntimeException {
    public TooManySessionsException() {
        super("Maximum number of active sessions reached. Please log out from another device");
    }
}
