package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProperties;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtProvider;
import io.github.blairjeon.stockflow.infrastructure.jwt.JwtUserInfo;
import io.github.blairjeon.stockflow.infrastructure.redis.RefreshTokenStore;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserStatus;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private JwtProvider jwtProvider;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenStore refreshTokenStore;
    @InjectMocks private AuthService authService;

    @Spy
    private JwtProperties jwtProperties =
            new JwtProperties(
                    "test-secret",
                    Duration.ofMinutes(15),
                    Duration.ofDays(7)
            );
    private User existsUser;

    @BeforeEach
    void setUp(){
        existsUser = User.create(
                "test-login-id",
                "test-password-hash"
        );
        ReflectionTestUtils.setField(existsUser, "id", 1L);
    }

    @Test
    void shouldLoginWhenRequestIdValid() {
        LoginRequest request =
                new LoginRequest("test-login-id", "test-password");

        when(userRepository.findByLoginId("test-login-id"))
                .thenReturn(Optional.of(existsUser));
        when(passwordEncoder.matches("test-password", "test-password-hash"))
                .thenReturn(true);
        when(jwtProvider.createAccessToken(any(JwtUserInfo.class)))
                .thenReturn("test-access-token");
        when(jwtProvider.createRefreshToken(1L))
                .thenReturn("test-refresh-token");

        TokenResponse response = authService.login(request);

        verify(userRepository).findByLoginId("test-login-id");
        verify(passwordEncoder).matches("test-password", "test-password-hash");
        verify(jwtProvider).createAccessToken(any());
        verify(jwtProvider).createRefreshToken(any());

        assertAll(
                () -> assertEquals("test-access-token", response.accessToken()),
                () -> assertEquals("test-refresh-token", response.refreshToken())
        );
    }

    @Test
    void shouldRejectLoginWhenUserNotFound(){
        LoginRequest request =
                new LoginRequest("test-worng-login-id", "test-password");

        when(userRepository.findByLoginId("test-worng-login-id"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        verify(userRepository).findByLoginId("test-worng-login-id");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(refreshTokenStore);

        assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    @Test
    void shouldRejectLoginWhenUserStatusNotActive(){
        LoginRequest request =
                new LoginRequest("test-login-id", "test-password");
        ReflectionTestUtils.setField(existsUser, "status", UserStatus.SUSPENDED);

        when(userRepository.findByLoginId("test-login-id"))
                .thenReturn(Optional.of(existsUser));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        verify(userRepository).findByLoginId("test-login-id");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(refreshTokenStore);

        assertEquals(ErrorCode.USER_SUSPENDED, exception.getErrorCode());
    }

    @Test
    void shouldRejectLoginWhenWrongPassword(){
        LoginRequest request =
                new LoginRequest("test-login-id", "test-wrong-password");

        when(userRepository.findByLoginId("test-login-id"))
                .thenReturn(Optional.of(existsUser));
        when(passwordEncoder.matches("test-wrong-password", "test-password-hash"))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        verify(userRepository).findByLoginId("test-login-id");
        verify(passwordEncoder).matches("test-wrong-password", "test-password-hash");
        verifyNoInteractions(refreshTokenStore);
        verifyNoInteractions(jwtProvider);

        assertEquals(ErrorCode.LOGIN_FAILED, exception.getErrorCode());
    }

    @Test
    void shouldCreateRefreshTokenWhenRequestIsValid() {
        String requestRefreshToken = "request-refresh-token";
        String newAccessToken = "test-access-token";
        String newRefreshToken = "test-refresh-token";

        when(jwtProvider.parseRefreshToken("request-refresh-token"))
                .thenReturn(1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existsUser));
        when(jwtProvider.createAccessToken(any(JwtUserInfo.class)))
                .thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(1L))
                .thenReturn(newRefreshToken);
        when(refreshTokenStore.rotate(
                1L,
                requestRefreshToken,
                newRefreshToken,
                jwtProperties.refreshExpiration()
        )).thenReturn(true);

        TokenResponse response =
                authService.refreshCreateToken(requestRefreshToken);

        assertAll(
                () -> assertEquals(newAccessToken, response.accessToken()),
                () -> assertEquals(newRefreshToken, response.refreshToken())
        );

        verify(refreshTokenStore).rotate(
                1L,
                requestRefreshToken,
                newRefreshToken,
                jwtProperties.refreshExpiration()
        );
    }

    @Test
    void shouldRejectWhenRefreshTokenIsInvalid(){
        String requestRefreshToken = "request-refresh-token";
        BusinessException tokenException = new BusinessException(ErrorCode.INVALID_TOKEN);

        when(jwtProvider.parseRefreshToken("request-refresh-token"))
                .thenThrow(tokenException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshCreateToken(requestRefreshToken)
        );

        verify(jwtProvider).parseRefreshToken("request-refresh-token");
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenUserDoesNotExist(){
        String requestRefreshToken = "request-refresh-token";

        when(jwtProvider.parseRefreshToken("request-refresh-token"))
                .thenReturn(2L);
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshCreateToken(requestRefreshToken)
        );

        verify(jwtProvider).parseRefreshToken("request-refresh-token");
        verify(userRepository).findById(2L);
        verify(jwtProvider, never()).createAccessToken(any());
        verify(jwtProvider, never()).createRefreshToken(any());
        verifyNoInteractions(refreshTokenStore);

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenUserStatusIsSuspended(){
        String requestRefreshToken = "request-refresh-token";
        ReflectionTestUtils.setField(existsUser, "status", UserStatus.SUSPENDED);

        when(jwtProvider.parseRefreshToken("request-refresh-token"))
                .thenReturn(1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existsUser));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshCreateToken(requestRefreshToken)
        );

        verify(jwtProvider).parseRefreshToken("request-refresh-token");
        verify(userRepository).findById(1L);
        verify(jwtProvider, never()).createAccessToken(any());
        verify(jwtProvider, never()).createRefreshToken(any());
        verifyNoInteractions(refreshTokenStore);

        assertEquals(ErrorCode.USER_SUSPENDED, exception.getErrorCode());
    }

    @Test
    void shouldRejectWhenTokenRotationFails(){
        String requestRefreshToken = "request-refresh-token";
        String newAccessToken = "test-access-token";
        String newRefreshToken = "test-refresh-token";

        when(jwtProvider.parseRefreshToken("request-refresh-token"))
                .thenReturn(1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(existsUser));
        when(jwtProvider.createAccessToken(any(JwtUserInfo.class)))
                .thenReturn(newAccessToken);
        when(jwtProvider.createRefreshToken(1L))
                .thenReturn(newRefreshToken);
        when(refreshTokenStore.rotate(
                1L,
                requestRefreshToken,
                newRefreshToken,
                jwtProperties.refreshExpiration()
        )).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.refreshCreateToken(requestRefreshToken)
        );

        verify(jwtProvider).parseRefreshToken("request-refresh-token");
        verify(userRepository).findById(1L);
        verify(jwtProvider).createAccessToken(any());
        verify(jwtProvider).createRefreshToken(any());
        verify(refreshTokenStore).rotate(1L, requestRefreshToken, newRefreshToken, Duration.ofDays(7));

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }
}