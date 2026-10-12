package io.github.blairjeon.stockflow.auth;

import io.github.blairjeon.stockflow.auth.dto.LoginRequest;
import io.github.blairjeon.stockflow.auth.dto.RefreshTokenRequest;
import io.github.blairjeon.stockflow.auth.dto.TokenResponse;
import io.github.blairjeon.stockflow.auth.exception.InvalidCredentialsException;
import io.github.blairjeon.stockflow.security.jwt.InvalidTokenException;
import io.github.blairjeon.stockflow.security.jwt.JwtProvider;
import io.github.blairjeon.stockflow.user.User;
import io.github.blairjeon.stockflow.user.UserRepository;
import io.github.blairjeon.stockflow.user.UserRole;
import io.github.blairjeon.stockflow.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldReturnTokensWhenCredentialsAreValid(){
        User user = User.create("alice", "encoded-password", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        LoginRequest request = new LoginRequest("alice", "password");

        when(userRepository.findByLoginId("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password"))
                .thenReturn(true);
        when(jwtProvider.createAccessToken(1L, UserRole.USER))
                .thenReturn("access-token");
        when(jwtProvider.createRefreshToken(1L))
                .thenReturn("refresh-token");

        TokenResponse response = authService.login(request);

        verify(userRepository).findByLoginId("alice");
        verify(passwordEncoder).matches("password", "encoded-password");
        verify(jwtProvider).createAccessToken(1L, UserRole.USER);
        verify(jwtProvider).createRefreshToken(1L);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void shouldRejectLoginWhenPasswordMismatches(){
        User user = User.create("alice", "encoded-password", "Alice");
        LoginRequest request = new LoginRequest("alice", "wrong-password");

        when(userRepository.findByLoginId("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verify(userRepository).findByLoginId("alice");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void shouldRejectLoginWhenUserNotFound(){
        LoginRequest request = new LoginRequest("alice", "password");

        when(userRepository.findByLoginId("alice")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verify(userRepository).findByLoginId("alice");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void shouldRefreshAccessTokenWhenRefreshTokenIsValid(){
        User user = User.create("alice", "encoded-password", "Alice");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtProvider.parseRefreshToken("refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(1L, UserRole.USER)).thenReturn("access-token");

        String accessToken = authService.refreshAccessToken(request);

        verify(jwtProvider).parseRefreshToken("refresh-token");
        verify(userRepository).findById(1L);
        verify(jwtProvider).createAccessToken(1L, UserRole.USER);

        assertEquals("access-token", accessToken);
    }

    @Test
    void shouldNotIssueAccessTokenWhenRefreshTokenParsingFails(){
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtProvider.parseRefreshToken("refresh-token")).thenThrow(InvalidTokenException.class);

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken(request));

        verifyNoInteractions(userRepository);
        verify(jwtProvider, never()).createAccessToken(any(), any());
    }

    @Test
    void shouldRejectRefreshWhenUserDoesNotExist(){
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(jwtProvider.parseRefreshToken("refresh-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.refreshAccessToken(request));

        verify(jwtProvider).parseRefreshToken("refresh-token");
        verify(userRepository).findById(1L);
        verify(jwtProvider, never()).createAccessToken(any(), any());
    }
}