package com.wallet.wallet_service.service;

import com.wallet.wallet_service.client.UserClient;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
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

        boolean userExist = userClient.checkUserExist(userId);

        if (!userExist) {
            throw new RuntimeException("Kullanıcı sisteme kayıtlı değil!");
        }


        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.TEN);

        String newAccountNumber = "ACC-" + System.currentTimeMillis();
        account.setAccountNumber(newAccountNumber);
        return accountRepository.save(account);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional
    public Account depositMoney(String accountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Yatırılacak tutar 0dan büyük olmalıdır");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Hesap bulunamadfdı"));

        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public void transferMoney(String fromNo, String toNo, BigDecimal amount) {
        Account from = accountRepository.findByAccountNumber(fromNo)
                .orElseThrow(() -> new RuntimeException("Kaynak hesap yok"));
        Account to = accountRepository.findByAccountNumber(toNo)
                .orElseThrow(() -> new RuntimeException("Hedef hesap yok"));

        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Bakiye yetersiz");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);
    }
}
