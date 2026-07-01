package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.CreditTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    boolean existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
            UUID idempotencyKey, Long customerId, OffsetDateTime after);
}
