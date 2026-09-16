package com.banking.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.mapper.AccountMapper;
import com.banking.account.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private KafkaAccountProducer kafkaAccountProducer;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void shouldCreateNewAccount() {
        UUID userId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(userId, new BigDecimal("100.00"), "usd");
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .balance(new BigDecimal("100.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(accountRepository.existsByUserIdAndCurrency(userId, "USD")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(new AccountResponse(account.getId(), userId, new BigDecimal("100.00"), "USD", AccountStatus.ACTIVE, 0L, account.getCreatedAt()));

        AccountResponse response = accountService.createAccount(request);

        assertEquals(userId, response.userId());
        assertEquals("USD", response.currency());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldRejectDebitWhenBalanceIsInsufficient() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder()
                .id(accountId)
                .userId(UUID.randomUUID())
                .balance(new BigDecimal("40.00"))
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class, () -> accountService.debit(accountId, new BigDecimal("50.00")));
    }
}
