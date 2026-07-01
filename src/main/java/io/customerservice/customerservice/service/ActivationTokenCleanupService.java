package io.customerservice.customerservice.service;

import io.customerservice.customerservice.repository.ActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivationTokenCleanupService {

    private final ActivationTokenRepository activationTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        try {
            long deleted = activationTokenRepository.deleteByExpiredAtBefore(OffsetDateTime.now());
            log.info("cleanup.activation_tokens.done deleted={}", deleted);
        } catch (Exception e) {
            log.error("cleanup.activation_tokens.failed", e);
        }
    }
}