package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/**
 * İstek biçim olarak geçerli ama bir iş kuralını ihlal ediyor (örn. yetersiz bakiye) → 422.
 * {@code errorCode} kuralı tanımlamalıdır, örn. {@code INSUFFICIENT_BALANCE}.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
    }
}
