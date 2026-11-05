package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/**
 * İstemciden gelen girdi geçersiz → 400.
 * <p>
 * {@link IllegalArgumentException} yerine bu sınıf kullanılmalıdır: kütüphanelerin fırlattığı
 * {@code IllegalArgumentException}'lar genellikle sunucu hatasıdır ve 400 olarak dönmemelidir.
 */
public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
}
