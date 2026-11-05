package com.wallet.user_service.service;

import com.wallet.common.exception.ConflictException;
import com.wallet.common.exception.InvalidCredentialsException;
import com.wallet.common.exception.ResourceNotFoundException;
import com.wallet.user_service.dto.UserCreatedEvent;
import com.wallet.user_service.entity.User;
import com.wallet.user_service.messaging.OutboxWriter;
import com.wallet.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxWriter outboxWriter;

    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ConflictException("USERNAME_ALREADY_EXISTS", "Bu kullanıcı adı zaten kullanılıyor");
        }
        // Not: Kontrol ile kayıt arasında aynı adla eşzamanlı bir kayıt gelirse veritabanının UNIQUE kısıtı
        // DataIntegrityViolationException fırlatır; GlobalExceptionHandler bunu da 409 olarak döner.

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        outboxWriter.write("user-created-topic", savedUser.getId().toString(),
                new UserCreatedEvent(savedUser.getId(), savedUser.getEmail()));

        return savedUser;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));
    }

    /**
     * Kullanıcı adı ve şifreyi doğrular. Kullanıcı yoksa da şifre yanlışsa da aynı hata döner;
     * böylece hangi kullanıcı adlarının kayıtlı olduğu dışarıdan anlaşılamaz.
     */
    @Transactional(readOnly = true)
    public User authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    public boolean existById(Long id) {
        return userRepository.existsById(id);
    }
}
