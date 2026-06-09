package com.wallet.wallet_service.client;

import com.wallet.wallet_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/api/users/check/{id}")
    boolean checkUserExist(@PathVariable("id") Long id);
}
