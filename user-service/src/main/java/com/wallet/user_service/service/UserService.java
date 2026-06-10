package com.wallet.user_service.service;

import com.wallet.user_service.entity.User;
import com.wallet.user_service.messaging.UserProducer;
import com.wallet.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProducer userProducer;

    public User createUser(User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userRepository.save(user);

            // Kafka gönderimini korumaya alalım
            try {
                userProducer.sendUserCreatedEvent(savedUser.getId(), savedUser.getEmail());
            } catch (Exception e) {
                System.err.println("Kafka mesajı gönderilemedi, ancak kullanıcı kaydedildi: " + e.getMessage());
            }

            return savedUser;
        } catch (Exception e) {
            // Hatanın tam tipini ve mesajını loglara dökelim
            e.printStackTrace();
            throw e;
        }
    }

    public User findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }

    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }

    public boolean existById(Long id) {
        return userRepository.existsById(id);
    }
}
