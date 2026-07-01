package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.internal.InternalBonusAccountChangeRequest;
import io.customerservice.customerservice.dto.response.BonusAccountResponse;
import io.customerservice.customerservice.entity.BonusAccount;
import io.customerservice.customerservice.exception.DuplicateIdempotencyKeyException;
import io.customerservice.customerservice.exception.InsufficientBalanceException;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.BonusAccountRepository;
import io.customerservice.customerservice.repository.CreditTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BonusServiceTest {

    @Mock BonusAccountRepository bonusAccountRepository;
    @Mock CreditTransactionRepository creditTransactionRepository;

    BonusService bonusService;

    private static final AppProperties APP_PROPERTIES = new AppProperties(
            new AppProperties.Jwt("dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHk=", 900000L),
            new AppProperties.Activation("http://localhost/activate", "http://localhost/success", "http://localhost/fail", 24),
            new AppProperties.Security(5, 7, 24),
            new AppProperties.Agreements(List.of()),
            new AppProperties.Messages("", ""),
            new AppProperties.Cleanup(new AppProperties.Cleanup.RefreshToken(1000)),
            new AppProperties.Internal("test-api-key")
    );

    @BeforeEach
    void setUp() {
        bonusService = new BonusService(bonusAccountRepository, creditTransactionRepository, APP_PROPERTIES.security());
    }

    @Test
    void getBalance_success() {
        BonusAccount account = bonusAccountWithBalance(100);
        when(bonusAccountRepository.findByCustomerId(1L)).thenReturn(Optional.of(account));

        BonusAccountResponse response = bonusService.getBalance(1L);

        assertThat(response.balance()).isEqualTo(100);
    }

    @Test
    void getBalance_accountNotFound_throws() {
        when(bonusAccountRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonusService.getBalance(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void credit_success() {
        BonusAccount account = bonusAccountWithBalance(50);
        InternalBonusAccountChangeRequest req = new InternalBonusAccountChangeRequest(100, UUID.randomUUID());
        when(bonusAccountRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(creditTransactionRepository.existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
                any(), eq(1L), any())).thenReturn(false);
        when(bonusAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BonusAccountResponse response = bonusService.credit(1L, req);

        assertThat(response.balance()).isEqualTo(150);
    }

    @Test
    void credit_duplicateIdempotencyKey_throws() {
        BonusAccount account = bonusAccountWithBalance(50);
        InternalBonusAccountChangeRequest req = new InternalBonusAccountChangeRequest(100, UUID.randomUUID());
        when(bonusAccountRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(creditTransactionRepository.existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
                any(), anyLong(), any())).thenReturn(true);

        assertThatThrownBy(() -> bonusService.credit(1L, req))
                .isInstanceOf(DuplicateIdempotencyKeyException.class);
    }

    @Test
    void redeem_success() {
        BonusAccount account = bonusAccountWithBalance(200);
        InternalBonusAccountChangeRequest req = new InternalBonusAccountChangeRequest(50, UUID.randomUUID());
        when(bonusAccountRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(creditTransactionRepository.existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
                any(), anyLong(), any())).thenReturn(false);
        when(bonusAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BonusAccountResponse response = bonusService.redeem(1L, req);

        assertThat(response.balance()).isEqualTo(150);
    }

    @Test
    void redeem_insufficientBalance_throws() {
        BonusAccount account = bonusAccountWithBalance(10);
        InternalBonusAccountChangeRequest req = new InternalBonusAccountChangeRequest(100, UUID.randomUUID());
        when(bonusAccountRepository.findByCustomerIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(creditTransactionRepository.existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
                any(), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> bonusService.redeem(1L, req))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    private static BonusAccount bonusAccountWithBalance(int balance) {
        BonusAccount account = new BonusAccount();
        account.setBalance(balance);
        return account;
    }
}
