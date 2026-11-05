package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Uygulamanın bilinçli olarak fırlattığı tüm hataların temel sınıfı.
 * <p>
 * Her alt sınıf bir HTTP durum kodu ve makine tarafından okunabilir bir {@code errorCode} taşır.
 * {@link GlobalExceptionHandler} bu bilgilerle RFC 9457 Problem Details cevabı üretir.
 * Mesaj ({@code detail}) doğrudan istemciye gösterildiği için hassas bilgi içermemelidir.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
