package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/** İstek mevcut durumla çakışıyor (örn. kullanıcı adı zaten alınmış) → 409. */
public class ConflictException extends ApiException {

    public ConflictException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
