package com.wallet.user_service.dto;

public record UserCreatedEvent(Long userId, String email) {}