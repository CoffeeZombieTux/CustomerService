package io.customerservice.customerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoOpEmailService implements EmailService {

    @Async
    @Override
    public void sendActivationEmail(String toEmail, String activationLink) {
        // Dev stub — logs PII (email + activation link) for local testing convenience.
        // A real implementation must not log these values.
        log.info("Activation link for {}: {}", toEmail, activationLink);
    }
}
