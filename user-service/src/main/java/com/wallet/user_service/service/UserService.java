package com.wallet.user_service.service;

import com.wallet.user_service.entity.User;
import com.wallet.user_service.messaging.UserProducer;
import com.wallet.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProducer userProducer;

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        userProducer.sendUserCreatedEvent(savedUser.getId(), savedUser.getEmail());

        return savedUser;
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
