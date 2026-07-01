package io.customerservice.customerservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) {
        super("The requested resource was not found: "  + resource);
    }
}
