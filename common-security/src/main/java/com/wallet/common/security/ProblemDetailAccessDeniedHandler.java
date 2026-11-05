package com.wallet.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Kimliği doğrulanmış ama yetkisi olmayan istekler (örn. USER rolüyle para yatırma) için 403 Problem Details yazar.
 * SecurityConfig'teki URL kurallarından gelen hataları karşılar; servis katmanındaki yetki hatalarını
 * {@code GlobalExceptionHandler} karşılar.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetails.write(request, response, objectMapper, ProblemDetails.of(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Bu işlem için yetkiniz yok"));
    }
}
