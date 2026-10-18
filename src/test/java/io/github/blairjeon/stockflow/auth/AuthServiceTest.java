package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProperties;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProvider;
import io.github.blairjeon.stockflow.infrastructure.redis.RefreshTokenStore;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private JwtProvider jwtProvider;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenStore refreshTokenStore;
    @InjectMocks private AuthService authService;

    private JwtProperties jwtProperties;

    @Test
    void shouldLoginWhenRequestIdValid(){

    }
}