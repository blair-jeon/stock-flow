package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProperties;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProvider;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtUserInfo;
import io.github.blairjeon.stockflow.infrastructure.redis.RefreshTokenStore;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtProperties jwtProperties;

    public TokenResponse login(LoginRequest request){
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if(!user.getStatus().equals(UserStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        TokenResponse tokens = createTokenPair(user);
        refreshTokenStore.save(
                user.getId(),
                tokens.refreshToken(),
                jwtProperties.refreshExpiration()
        );

        return tokens;
    }

    public TokenResponse refreshCreateToken(String refreshToken){
        Long userId = jwtProvider.parseRefreshToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(!user.getStatus().equals(UserStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        TokenResponse tokens = createTokenPair(user);

        boolean rotated = refreshTokenStore.rotate(
                user.getId(),
                refreshToken,
                tokens.refreshToken(),
                jwtProperties.refreshExpiration()
        );

        if(!rotated){
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        return tokens;
    }

    public void logout(Long userId){
        refreshTokenStore.delete(userId);
    }

    private TokenResponse createTokenPair(User user){
        JwtUserInfo userInfo = JwtUserInfo.from(user);

        return new TokenResponse(
                jwtProvider.createAccessToken(userInfo),
                jwtProvider.createRefreshToken(userInfo.userId())
        );
    }
}