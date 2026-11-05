package com.wallet.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Kullanıcı adı boş olamaz")
        String username,

        @NotBlank(message = "Şifre boş olamaz")
        String password
) {
}
