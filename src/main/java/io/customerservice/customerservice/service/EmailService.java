package io.customerservice.customerservice.service;


/**
 *  Use Async annotation on each implementation.
 *  Async is allowed by io/customerservice/customerservice/config/AsyncConfig.java
 */
public interface EmailService {
    void sendActivationEmail(String toEmail, String activationLink);
}