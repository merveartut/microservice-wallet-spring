package com.wallet.wallet_service.controller;

import com.wallet.wallet_service.dto.TransferRequest;
import com.wallet.wallet_service.entity.Account;
import com.wallet.wallet_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<String> createAccount(@RequestParam Long userId, @RequestParam String currency) {
        Account createdAccount = accountService.createAccount(userId, currency);

        return ResponseEntity.ok("Hesap başarılı bir şekilde oluşturuldu. Hesap no: "+ createdAccount.getAccountNumber());
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount) {
        Account updatedAccount = accountService.depositMoney(accountNumber, amount);
        return ResponseEntity.ok(("Para yatırıldı. Guncel bakiyeniz:" + updatedAccount.getBalance()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccount(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferRequest request) {
        accountService.transferMoney(
                request.fromAccount(),
                request.toAccount(),
                request.amount()
        );

        return ResponseEntity.ok("Transfer başarıyla gerçekleşti.");
    }

}
