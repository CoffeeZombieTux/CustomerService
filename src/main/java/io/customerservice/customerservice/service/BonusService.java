package io.customerservice.customerservice.service;

import io.customerservice.customerservice.config.AppProperties;
import io.customerservice.customerservice.dto.request.internal.InternalBonusAccountChangeRequest;
import io.customerservice.customerservice.dto.response.BonusAccountResponse;
import io.customerservice.customerservice.entity.BonusAccount;
import io.customerservice.customerservice.entity.CreditTransaction;
import io.customerservice.customerservice.entity.TransactionType;
import io.customerservice.customerservice.exception.DuplicateIdempotencyKeyException;
import io.customerservice.customerservice.exception.InsufficientBalanceException;
import io.customerservice.customerservice.exception.ResourceNotFoundException;
import io.customerservice.customerservice.repository.BonusAccountRepository;
import io.customerservice.customerservice.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BonusService {

    private final BonusAccountRepository bonusAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AppProperties.Security securityProps;

    public BonusAccountResponse getBalance(Long customerId) {
        BonusAccount bonusAccount = bonusAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(BonusAccount.RESOURCE));
        return new BonusAccountResponse(bonusAccount.getBalance());
    }

    @Transactional
    public BonusAccountResponse credit(Long customerId, InternalBonusAccountChangeRequest creditRequest) {
        BonusAccount bonusAccount = changeBalance(customerId, creditRequest, true);

        log.info("bonus.credit customerId={} amount={} idempotencyKey={}",
                customerId,
                creditRequest.amount(),
                creditRequest.idempotencyKey()
        );
        return new BonusAccountResponse(bonusAccount.getBalance());
    }

    @Transactional
    public BonusAccountResponse redeem(Long customerId, InternalBonusAccountChangeRequest redeemRequest) {
        BonusAccount bonusAccount = changeBalance(customerId, redeemRequest, false);
        log.info("bonus.redeem customerId={} amount={} idempotencyKey={}",
                customerId,
                redeemRequest.amount(),
                redeemRequest.idempotencyKey()
        );
        return new BonusAccountResponse(bonusAccount.getBalance());
    }

    private BonusAccount changeBalance(Long customerId, InternalBonusAccountChangeRequest request, boolean isCreditOperation) {
        BonusAccount bonusAccount = bonusAccountRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(BonusAccount.RESOURCE));
        if (creditTransactionRepository.existsByIdempotencyKeyAndBonusAccount_CustomerIdAndAudit_CreatedAtAfter(
                request.idempotencyKey(), customerId, OffsetDateTime.now().minusHours(securityProps.idempotencyKeyHours()))) {
            throw new DuplicateIdempotencyKeyException();
        }
        CreditTransaction transaction = new CreditTransaction();

        if (isCreditOperation) {
            bonusAccount.setBalance(bonusAccount.getBalance() + request.amount());
            transaction.setTransactionType(TransactionType.EARN);
        } else {
            if ((bonusAccount.getBalance() - request.amount()) < 0) {
                throw new InsufficientBalanceException();
            }
            bonusAccount.setBalance(bonusAccount.getBalance() - request.amount());
            transaction.setTransactionType(TransactionType.REDEEM);
        }
        transaction.setAmount(request.amount());
        transaction.setIdempotencyKey(request.idempotencyKey());
        transaction.setBonusAccount(bonusAccount);
        creditTransactionRepository.save(transaction);
        return bonusAccountRepository.save(bonusAccount);
    }
}
