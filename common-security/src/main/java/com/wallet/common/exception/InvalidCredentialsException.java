package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Giriş bilgileri hatalı → 401.
 * <p>
 * Kullanıcı adının yanlış mı yoksa şifrenin mi yanlış olduğu bilinçli olarak ayırt edilmez;
 * aksi halde sistemde hangi kullanıcı adlarının kayıtlı olduğu tahmin edilebilir.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Kullanıcı adı veya şifre hatalı");
    }
}
