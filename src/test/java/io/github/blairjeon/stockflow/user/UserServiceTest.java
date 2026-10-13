package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.user.dto.*;
import io.github.blairjeon.stockflow.user.exception.DuplicateLoginIdException;
import io.github.blairjeon.stockflow.user.exception.PasswordMismatchException;
import io.github.blairjeon.stockflow.user.exception.UserNotFoundException;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private User existingUser(){
        User user = User.create("alice", "encoded-password", "Alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        return user;
    }

    @Test
    void shouldSaveUserWithEncodedPasswordWhenCreated() {
        UserCreateRequest request = new UserCreateRequest("alice", "test-password", "Alice");

        when(userRepository.existsByLoginId("alice")).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.create(request);

        verify(userRepository).saveAndFlush(userCaptor.capture());
        verify(userRepository).existsByLoginId("alice");
        verify(passwordEncoder).encode("test-password");

        User savedUser = userCaptor.getValue();
        assertEquals("alice", savedUser.getLoginId());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals("Alice", savedUser.getDisplayName());
    }

    @Test
    void shouldRejectDuplicateLoginIdBeforeSavingUser(){
        UserCreateRequest request = new UserCreateRequest("alice", "test-password", "Alice");

        when(userRepository.existsByLoginId("alice"))
                .thenReturn(true);

        assertThrows(
                DuplicateLoginIdException.class,
                () -> userService.create(request)
        );

        verify(userRepository, never()).saveAndFlush(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectDuplicateLoginIdOnSave(){
        UserCreateRequest request = new UserCreateRequest("alice", "test-password", "Alice");
        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate entry",
                new SQLException("Duplicate entry"),
                "uk_users_loginId"
        );

        DataIntegrityViolationException exception = new DataIntegrityViolationException("DB constrain violation", cause);

        when(userRepository.existsByLoginId("alice")).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(exception);

        assertThrows(DuplicateLoginIdException.class, ()->userService.create(request));

        verify(userRepository).existsByLoginId("alice");
        verify(passwordEncoder).encode("test-password");
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void shouldReturnUserWhenFound(){
        User user = existingUser();

        UserResponse response = userService.get(1L);

        assertEquals("alice", response.loginId());
        assertEquals("Alice", response.displayName());

        verify(userRepository).findById(1L);
    }

    @Test
    void shouldThrowUserNotFoundWhenUserDoesNotExist(){
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.get(2L)
        );

        verify(userRepository).findById(2L);
    }

    @Test
    void shouldUpdatePasswordWhenOldPasswordMatches(){
        User user = existingUser();
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "old-password",
                "new-password"
        );

        when(passwordEncoder.matches("old-password", "encoded-password"))
                .thenReturn(true);
        when(passwordEncoder.encode("new-password"))
                .thenReturn("new-encoded-password");

        userService.updatePassword(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("old-password", "encoded-password");
        verify(passwordEncoder).encode("new-password");

        assertEquals("new-encoded-password", user.getPasswordHash());
    }

    @Test
    void shouldRejectPasswordUpdateWhenOldPasswordMismatches(){
        User user = existingUser();
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "old-wrong-password",
                "new-password"
        );

        when(passwordEncoder.matches("old-wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThrows(PasswordMismatchException.class,
                () -> userService.updatePassword(1L, request));

        verify(passwordEncoder).matches("old-wrong-password", "encoded-password");
        verify(passwordEncoder, never()).encode(any(String.class));
        assertEquals("encoded-password", user.getPasswordHash());
    }

    @Test
    void shouldRejectPasswordUpdateWhenUserNotFound(){
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "old-wrong-password",
                "new-password"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updatePassword(1L, request));

        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldUpdateDisplayNameWhenPasswordMatches(){
        User user = existingUser();
        UserDisplayNameUpdateRequest request = new UserDisplayNameUpdateRequest("password", "Alice-updated");

        when(passwordEncoder.matches("password", "encoded-password"))
                .thenReturn(true);

        userService.updateDisplayName(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("password", "encoded-password");
        assertEquals("Alice-updated", user.getDisplayName());
    }

    @Test
    void shouldRejectDisplayNameUpdateWhenPasswordMismatches(){
        User user = existingUser();
        UserDisplayNameUpdateRequest request = new UserDisplayNameUpdateRequest("wrong-password", "Alice-updated");

        when(passwordEncoder.matches("wrong-password", "encoded-password"))
                .thenReturn(false);

        assertThrows(PasswordMismatchException.class,
                () -> userService.updateDisplayName(1L, request));

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        assertEquals("Alice", user.getDisplayName());
    }

    @Test
    void shouldRejectDisplayNameUpdateWhenUserNotFound() {
        UserDisplayNameUpdateRequest request = new UserDisplayNameUpdateRequest("password", "Alice-updated");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.updateDisplayName(1L, request));

        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldDeleteUserWhenPasswordMatches(){
        User user = existingUser();
        UserDeleteRequest request = new UserDeleteRequest("password");

        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        userService.delete(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("password", "encoded-password");
        verify(userRepository).delete(same(user));
    }

    @Test
    void shouldRejectUserDeleteWhenPasswordMismatches(){
        User user = existingUser();
        UserDeleteRequest request = new UserDeleteRequest("wrong-password");

        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(PasswordMismatchException.class,
                () -> userService.delete(1L, request));

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void shouldRejectUserDeleteWhenUserNotFound(){
        UserDeleteRequest request = new UserDeleteRequest("wrong-password");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.delete(1L, request));

        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).delete(any(User.class));
    }
}