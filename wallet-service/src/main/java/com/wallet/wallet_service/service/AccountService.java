package com.wallet.wallet_service.service;

import com.wallet.common.exception.BusinessRuleException;
import com.wallet.common.exception.InvalidRequestException;
import com.wallet.common.exception.ResourceNotFoundException;
import com.wallet.wallet_service.client.UserClient;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserClient userClient;

    public Account createAccount(Long userId, String currency) {
        if (!userClient.checkUserExist(userId)) {
            throw new ResourceNotFoundException("Kullanıcı bulunamadı");
        }

        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.TEN);
        return accountRepository.save(account);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    /** Sistemdeki tüm hesaplar (yalnızca ADMIN; yetki SecurityConfig'te kontrol edilir) */
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public Account depositMoney(String accountNumber, BigDecimal amount) {
        requirePositive(amount, "Yatırılacak tutar 0'dan büyük olmalıdır");

        Account account = lockAccount(accountNumber, "Hesap bulunamadı");

        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public void transferMoney(Long authenticatedUserId, String fromNo, String toNo, BigDecimal amount) {
        // HTTP katmanında @Valid ile de doğrulanıyor; bu kontrol HTTP dışından gelen çağrılar için
        requirePositive(amount, "Transfer tutarı 0'dan büyük olmalıdır");
        if (fromNo.equals(toNo)) {
            throw new InvalidRequestException("Kaynak ve hedef hesap aynı olamaz!");
        }
        // Deadlock'u önlemek için iki hesabı her zaman aynı sırada (hesap numarasına göre) kilitle
        Account from;
        Account to;
        if (fromNo.compareTo(toNo) < 0) {
            from = lockAccount(fromNo, "Kaynak hesap bulunamadı");
            to = lockAccount(toNo, "Hedef hesap bulunamadı");
        } else {
            to = lockAccount(toNo, "Hedef hesap bulunamadı");
            from = lockAccount(fromNo, "Kaynak hesap bulunamadı");
        }

        if (!from.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Bu hesaptan transfer yapma yetkiniz yok");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_BALANCE", "Yetersiz bakiye");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }

    private Account lockAccount(String accountNumber, String notFoundMessage) {
        return accountRepository.findWithLockByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private static void requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException(message);
        }
    }
}
