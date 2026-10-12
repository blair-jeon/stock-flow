package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.RefreshTokenRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import io.github.blairjeon.stockflow.auth.exception.InvalidCredentialsException;
import io.github.blairjeon.stockflow.security.jwt.JwtProvider;
import io.github.blairjeon.stockflow.user.*;
import io.github.blairjeon.stockflow.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public TokenResponse login(LoginRequest request){
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getUserRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }

    public String refreshAccessToken(RefreshTokenRequest request){
        Long userId = jwtProvider.parseRefreshToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return jwtProvider.createAccessToken(user.getId(), user.getUserRole());
    }
}