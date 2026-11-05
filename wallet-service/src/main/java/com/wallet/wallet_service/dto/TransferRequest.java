package com.wallet.wallet_service.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Kaynak hesap numarası boş olamaz")
        String fromAccount,

        @NotBlank(message = "Hedef hesap numarası boş olamaz")
        String toAccount,

        @NotNull(message = "Transfer tutarı zorunludur")
        @Positive(message = "Transfer tutarı 0'dan büyük olmalıdır")
        BigDecimal amount
) {
        @AssertTrue(message = "Kaynak ve hedef hesap aynı olamaz")
        public boolean isDifferentAccounts() {
                return fromAccount == null || !fromAccount.equals(toAccount);
        }

}
