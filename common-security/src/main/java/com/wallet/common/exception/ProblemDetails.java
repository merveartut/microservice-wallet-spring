package com.wallet.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * Tüm hata cevaplarının aynı biçimde (RFC 9457) üretilmesi için yardımcı metotlar.
 * <p>
 * Standart alanlara ({@code type, title, status, detail, instance}) ek olarak her cevapta
 * {@code errorCode} ve {@code timestamp} bulunur.
 */
public final class ProblemDetails {

    public static final String ERROR_CODE = "errorCode";
    public static final String TIMESTAMP = "timestamp";
    public static final String ERRORS = "errors";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatusCode status, String errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        enrich(problem, errorCode);
        return problem;
    }

    /** Spring'in ürettiği ProblemDetail'lere de errorCode ve timestamp eklemek için. */
    public static void enrich(ProblemDetail problem, String errorCode) {
        problem.setProperty(ERROR_CODE, errorCode);
        problem.setProperty(TIMESTAMP, Instant.now());
    }

    /**
     * MVC katmanı dışında (Spring Security filtreleri) hata cevabı yazmak için.
     * Controller'a ulaşmayan isteklerde @RestControllerAdvice devreye girmez.
     */
    public static void write(HttpServletRequest request, HttpServletResponse response,
                             ObjectMapper objectMapper, ProblemDetail problem) throws IOException {
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
