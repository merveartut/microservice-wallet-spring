package com.wallet.user_service.controller;

import com.wallet.user_service.dto.AuthRequest;
import com.wallet.user_service.dto.UserRequest;
import com.wallet.user_service.entity.User;
import com.wallet.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.wallet.common.security.JwtService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEmail(request.email());
        try {
            return ResponseEntity.ok(userService.createUser(user));
        } catch (Exception e) {
            // Hatanın tam olarak ne olduğunu loglara yazdır
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(user);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/check/{id}")
    public boolean checkUserExist(@PathVariable("id") Long id) {
        return userService.existById(id);
    }

    @PostMapping("/login")
    public String login(@RequestBody AuthRequest authRequest) {
        User user = userService.findByUsername(authRequest.username());

        if (passwordEncoder.matches(authRequest.password(), user.getPassword())) {
            return jwtService.generateToken(authRequest.username(), user.getId());
        } else {
            throw new RuntimeException("hatali parola girdiniz");
        }
    }
}
