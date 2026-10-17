package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProvider;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtUserInfo;
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

    public TokenResponse login(LoginRequest request){
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if(!user.getStatus().equals(UserStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return createToken(user);
    }

    public TokenResponse refreshCreateToken(String refreshToken){
        Long userId = jwtProvider.parseRefreshToken(refreshToken);

        // TODO: Refresh Token Rotation - Redis

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if(!user.getStatus().equals(UserStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }

        return createToken(user);
    }

    public void logout(){ }

    private TokenResponse createToken(User user){
        JwtUserInfo userInfo = JwtUserInfo.from(user);
        String accessToken = jwtProvider.createAccessToken(userInfo);
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken);
    }
}