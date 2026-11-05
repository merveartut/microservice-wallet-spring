package com.wallet.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Kimliği doğrulanmamış istekler (token yok, geçersiz veya süresi dolmuş) için 401 Problem Details yazar.
 * Spring Security'nin varsayılanı bu durumda boş gövdeli 403 dönüyordu.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetails.write(request, response, objectMapper, ProblemDetails.of(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Bu işlem için geçerli bir token gereklidir"));
    }
}
