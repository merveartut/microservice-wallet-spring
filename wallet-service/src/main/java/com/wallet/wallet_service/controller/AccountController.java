package com.wallet.wallet_service.controller;

import com.wallet.common.security.CurrentUser;
import com.wallet.wallet_service.dto.TransferRequest;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<String> createAccount(@RequestParam("currency") String currency) {
        Account createdAccount = accountService.createAccount(CurrentUser.id(), currency);
        return ResponseEntity.ok("Hesap başarılı bir şekilde oluşturuldu. Hesap no: " + createdAccount.getAccountNumber());
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount) {
        Account updatedAccount = accountService.depositMoney(accountNumber, amount);
        return ResponseEntity.ok("Para yatırıldı. Guncel bakiyeniz:" + updatedAccount.getBalance());
    }

    /** Yalnızca ADMIN: para yatırma ekranındaki hesap seçici için tüm hesaplar */
    @GetMapping("/accounts")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccount(@PathVariable("userId") Long userId) {
        if (!CurrentUser.id().equals(userId)) {
            throw new AccessDeniedException("Yalnızca kendi hesaplarınızı görüntüleyebilirsiniz");
        }
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transferMoney(
                CurrentUser.id(),
                request.fromAccount(),
                request.toAccount(),
                request.amount()
        );
        return ResponseEntity.ok("Transfer başarıyla gerçekleşti.");
    }
}
