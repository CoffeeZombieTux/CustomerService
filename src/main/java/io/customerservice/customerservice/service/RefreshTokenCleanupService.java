package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties.Cleanup cleanupProps;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        try {
            List<Long> ids;
            int total = 0;
            do {
                ids = refreshTokenRepository.findExpiredIds(
                        OffsetDateTime.now(), PageRequest.of(0, cleanupProps.refreshToken().batch()));
                if (!ids.isEmpty()) {
                    refreshTokenRepository.deleteByIdIn(ids);
                    total += ids.size();
                }
            } while (ids.size() == cleanupProps.refreshToken().batch());
            log.info("cleanup.refresh_tokens.done deleted={}", total);
        } catch (Exception e) {
            log.error("cleanup.refresh_tokens.failed", e);
        }
    }
}