package com.wallet.wallet_service.config;

import com.wallet.common.security.JwtAuthenticationFilter;
import com.wallet.common.security.ProblemDetailAccessDeniedHandler;
import com.wallet.common.security.ProblemDetailAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot'un hata sayfası; kapalı olursa her hata 401/403'e dönüşür
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/account/deposit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/account/accounts").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Token yok/geçersiz → 401, yetki yok → 403 (ikisi de Problem Details JSON)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
