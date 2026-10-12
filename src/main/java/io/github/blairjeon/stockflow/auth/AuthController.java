package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.RefreshTokenRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public String refreshAccessToken(@Valid @RequestBody RefreshTokenRequest refreshToken){
        return authService.refreshAccessToken(refreshToken);
    }

    // public void logout(){}
    // Redis 추가시 업데이트예정
}