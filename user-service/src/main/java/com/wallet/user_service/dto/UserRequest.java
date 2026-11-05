package com.wallet.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank(message = "Kullanıcı adı boş olamaz")
        String username,

        @NotBlank(message = "Şifre boş olamaz")
        String password,

        @Email(message = "Geçerli bir e-posta adresi giriniz")
        String email
) {
}
