package com.wallet.user_service.config;

import com.wallet.common.security.JwtAuthenticationFilter;
import com.wallet.common.security.ProblemDetailAccessDeniedHandler;
import com.wallet.common.security.ProblemDetailAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Sadece register ve login endpoint'lerine herkes erişebilir
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        // 2. Spring Boot'un hata sayfası; kapalı olursa her hata 401/403'e dönüşür
                        .requestMatchers("/error").permitAll()
                        // 3. Diğer tüm istekler için JWT doğrulaması şart
                        .anyRequest().authenticated()
                )
                // 4. Token yok/geçersiz → 401, yetki yok → 403 (ikisi de Problem Details JSON)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // 5. Stateless bir mimari (JWT) için Session management'ı kapatalım
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 6. JWT Filtresini, kullanıcı adı/şifre kontrol filtresinden önce çalıştır
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
