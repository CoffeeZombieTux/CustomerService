package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.BonusAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BonusAccountRepository extends JpaRepository<BonusAccount, Long> {
    Optional<BonusAccount> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BonusAccount b WHERE b.customer.id = :customerId")
    Optional<BonusAccount> findByCustomerIdForUpdate(@Param("customerId") Long customerId);
}