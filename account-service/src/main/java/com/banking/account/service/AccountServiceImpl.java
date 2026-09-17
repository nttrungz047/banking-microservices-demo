package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.DuplicateAccountException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.exception.InvalidAccountStateException;
import com.banking.account.mapper.AccountMapper;
import com.banking.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String normalizedCurrency = request.currency().trim().toUpperCase(Locale.ROOT);
        if (accountRepository.existsByUserIdAndCurrency(request.userId(), normalizedCurrency)) {
            throw new DuplicateAccountException("Account already exists for userId=" + request.userId() + " and currency=" + normalizedCurrency);
        }

        Account account = Account.builder()
                .userId(request.userId())
                .balance(request.initialBalance())
                .currency(normalizedCurrency)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Created account {} for user {} with balance {} {}", saved.getId(), saved.getUserId(), saved.getBalance(), saved.getCurrency());
        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        return accountMapper.toResponse(findAccount(accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AccountResponse debit(UUID accountId, BigDecimal amount) {
        Account account = findAccount(accountId);
        validateActive(account);
        validatePositiveAmount(amount);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance on account " + accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));

        try {
            Account saved = accountRepository.saveAndFlush(account);
            log.info("Debited account {} by {}. New balance {}", accountId, amount, saved.getBalance());
            return accountMapper.toResponse(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new com.banking.account.exception.OptimisticLockingFailureException("Account is being updated concurrently: " + accountId, ex);
        }
    }

    @Override
    @Transactional
    public AccountResponse credit(UUID accountId, BigDecimal amount) {
        Account account = findAccount(accountId);
        validateActive(account);
        validatePositiveAmount(amount);

        account.setBalance(account.getBalance().add(amount));

        try {
            Account saved = accountRepository.saveAndFlush(account);
            log.info("Credited account {} by {}. New balance {}", accountId, amount, saved.getBalance());
            return accountMapper.toResponse(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new com.banking.account.exception.OptimisticLockingFailureException("Account is being updated concurrently: " + accountId, ex);
        }
    }

    private Account findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    private void validateActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException("Account is not active: " + account.getId());
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
