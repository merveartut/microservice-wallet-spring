package com.wallet.common.exception;

import org.springframework.http.HttpStatus;

/** İstenen kaynak (kullanıcı, hesap vb.) bulunamadı → 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
