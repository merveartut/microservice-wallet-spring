package com.wallet.wallet_service.dto;

import java.math.BigDecimal;

public record TransferRequest(String fromAccount, String toAccount, BigDecimal amount) {
}
