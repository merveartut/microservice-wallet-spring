package com.wallet.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * İsteği yapan kullanıcının kimliğine erişim.
 * {@link JwtAuthenticationFilter} principal olarak token'daki userId'yi (Long) koyar.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        // Korunan endpoint'lerde Security filtreleri bu noktaya gelinmesini zaten engeller.
        throw new AccessDeniedException("Kimlik doğrulanamadı");
    }
}
