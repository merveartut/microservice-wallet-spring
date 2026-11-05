package com.wallet.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

/**
 * Controller katmanından çıkan tüm hataları RFC 9457 Problem Details cevabına çevirir.
 * <p>
 * {@link ResponseEntityExceptionHandler}'dan türediği için Spring MVC'nin standart hataları
 * (eksik parametre, hatalı JSON, tip uyuşmazlığı, desteklenmeyen metot vb.) da aynı biçimde döner.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Uygulamanın bilinçli olarak fırlattığı hatalar. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        log.debug("İstek reddedildi [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ProblemDetails.of(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
    }

    /**
     * Servis katmanındaki yetki kontrolleri (örn. başkasının hesabından transfer).
     * URL seviyesindeki yetki hataları bu metoda gelmez; onları {@code ProblemDetailAccessDeniedHandler} yazar.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.debug("Erişim reddedildi: {}", ex.getMessage());
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
    }

    /** Veritabanı kısıt ihlali (örn. eşzamanlı aynı kullanıcı adıyla kayıt). SQL ayrıntısı istemciye verilmez. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Veri bütünlüğü ihlali: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetails.of(HttpStatus.CONFLICT, "DATA_CONFLICT", "İstek mevcut verilerle çakışıyor");
    }

    /** Beklenmeyen tüm hatalar. Ayrıntı yalnızca loglanır, istemciye genel bir mesaj döner. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Beklenmeyen bir hata oluştu");
    }

    /** @Valid ile doğrulanan istek gövdesi hataları: her alan için ayrı mesaj döner. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();

        ProblemDetail problem = ProblemDetails.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "İstek doğrulanamadı");
        problem.setProperty(ProblemDetails.ERRORS, errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Spring'in standart hatalarına da errorCode ve timestamp ekler.
     * Bazı hatalarda (eksik parametre, 404, 405) gövde süper sınıfta oluşturulduğu için
     * zenginleştirme super çağrısından sonra yapılır.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null
                && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ProblemDetails.ERROR_CODE))) {
            ProblemDetails.enrich(problem, defaultErrorCode(statusCode));
        }
        return response;
    }

    private static String defaultErrorCode(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "INVALID_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "HTTP_" + status.value();
        };
    }
}
