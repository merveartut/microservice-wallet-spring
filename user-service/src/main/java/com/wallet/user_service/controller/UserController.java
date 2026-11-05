package com.wallet.user_service.controller;

import com.wallet.common.security.CurrentUser;
import com.wallet.common.security.JwtService;
import com.wallet.user_service.dto.AuthRequest;
import com.wallet.user_service.dto.UserRequest;
import com.wallet.user_service.entity.User;
import com.wallet.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEmail(request.email());
        user.setRole("USER");
        return ResponseEntity.ok(userService.createUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        // Kontrol DB sorgusundan önce: başkasına ait var olan ve olmayan id'ler aynı cevabı alır
        if (!CurrentUser.id().equals(id)) {
            throw new AccessDeniedException("Yalnızca kendi kullanıcı bilgilerinizi görüntüleyebilirsiniz");
        }
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/check/{id}")
    public boolean checkUserExist(@PathVariable("id") Long id) {
        return userService.existById(id);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody AuthRequest authRequest) {
        User user = userService.authenticate(authRequest.username(), authRequest.password());
        return jwtService.generateToken(user.getUsername(), user.getId(), user.getRole());
    }
}
