package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.user.dto.*;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserProfile;
import io.github.blairjeon.stockflow.user.entity.UserRole;
import io.github.blairjeon.stockflow.user.entity.UserStatus;
import io.github.blairjeon.stockflow.user.repository.UserProfileRepository;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp(){
    }

    @Test
    void shouldCreateUserWhenRequestIsValid(){
        UserCreateRequest request = new UserCreateRequest(
                "test-user-loginId",
                "test-user-password"
        );
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByLoginId("test-user-loginId"))
                .thenReturn(false);
        when(passwordEncoder.encode("test-user-password"))
                .thenReturn("test-encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        verify(userRepository).existsByLoginId("test-user-loginId");
        verify(passwordEncoder).encode("test-user-password");
        verify(userRepository).saveAndFlush(captor.capture());

        User savedUser = captor.getValue();

        assertAll(
                () -> assertEquals("test-user-loginId", response.loginId()),
                () -> assertEquals(UserRole.USER, response.role()),
                () -> assertEquals(UserStatus.ACTIVE, response.status()),
                () -> assertEquals("test-user-loginId", savedUser.getLoginId()),
                () -> assertEquals("test-encoded-password", savedUser.getPasswordHash()),
                () -> assertEquals(UserRole.USER, savedUser.getRole()),
                () -> assertEquals(UserStatus.ACTIVE, savedUser.getStatus())
        );
    }

    @Test
    void shouldRejectSignupWhenLoginIdAlreadyExists(){
        UserCreateRequest request = new UserCreateRequest(
                "test-user-loginId",
                "test-password"
        );

        when(userRepository.existsByLoginId("test-user-loginId"))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> userService.create(request));

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectSignupWhenLoginIdUniqueConstrainIsViolated(){
        UserCreateRequest request = new UserCreateRequest(
                "test-user-loginId",
                "test-password"
        );

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate login ID",
                new SQLException("Duplicate entry"),
                "uk_users_login_id"
        );

        DataIntegrityViolationException dbException =
                new DataIntegrityViolationException("Insert failed", cause);

        when(userRepository.existsByLoginId("test-user-loginId"))
                .thenReturn(false);
        when(passwordEncoder.encode("test-password"))
                .thenReturn("test-encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(dbException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.create(request)
        );

        assertEquals(ErrorCode.LOGIN_ID_TAKEN, exception.getErrorCode());
    }

    @Test
    void shouldCreateProfileWhenRequestIsValid(){
        UserProfileCreateRequest request = new UserProfileCreateRequest(
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.existsById(1L))
                .thenReturn(true);
        when(userProfileRepository.existsById(1L))
                .thenReturn(false);
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.createProfile(1L, request);

        verify(userRepository).existsById(1L);
        verify(userProfileRepository).existsById(1L);
        verify(userProfileRepository).save(captor.capture());
        UserProfile savedProfile = captor.getValue();

        assertAll(
                () -> assertEquals(1L, savedProfile.getUserId()),
                () -> assertEquals("test-last-name", savedProfile.getLastName()),
                () -> assertEquals("test-first-name", savedProfile.getFirstName()),
                () -> assertEquals("000-000-0000", savedProfile.getPhoneNumber()),
                () -> assertEquals("test@email.com", savedProfile.getEmail()),
                () -> assertEquals("test-address", savedProfile.getAddress())
        );
    }

    @Test
    void shouldRejectCreateProfileWhenUserNotFound(){
        UserProfileCreateRequest request = new UserProfileCreateRequest(
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        when(userRepository.existsById(1L))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createProfile(1L, request)
        );

        verify(userRepository).existsById(1L);
        verifyNoInteractions(userProfileRepository);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectCreateProfileWhenProfileAlreadyExists(){
        UserProfileCreateRequest request = new UserProfileCreateRequest(
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        when(userRepository.existsById(1L))
                .thenReturn(true);
        when(userProfileRepository.existsById(1L))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createProfile(1L, request)
        );

        assertEquals(ErrorCode.USER_PROFILE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository).existsById(1L);
        verify(userProfileRepository).existsById(1L);
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void shouldGetUserWhenRequestIsValid(){
        User user = User.create(
                "test-loginId",
                "test-password-hash"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getUser(1L);

        assertAll(
                () -> assertEquals("test-loginId", response.loginId()),
                () -> assertEquals(UserRole.USER, response.role()),
                () -> assertEquals(UserStatus.ACTIVE, response.status())
        );
    }

    @Test
    void shouldRejectGetUserWhenUserNotFound(){
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getUser(1L));

        verify(userRepository).findById(1L);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldGetProfileWhenRequestIsValid(){
        UserProfile profile = UserProfile.create(
                1L,
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        when(userProfileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        UserProfileResponse response = userService.getProfile(1L);

        verify(userProfileRepository).findById(1L);
        assertAll(
                () -> assertEquals("test-last-name", response.lastName()),
                () -> assertEquals("test-first-name", response.firstName()),
                () -> assertEquals("000-000-0000", response.phoneNumber()),
                () -> assertEquals("test@email.com", response.email()),
                () -> assertEquals("test-address", response.address())
        );
    }

    @Test
    void shouldRejectGetProfileWhenProfileNotFound(){
        when(userProfileRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.getProfile(1L)
        );

        verify(userProfileRepository).findById(1L);
        assertEquals(ErrorCode.USER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldUpdatePasswordWhenRequestIsValid(){
        User user = User.create(
                "test-loginId",
                "test-password-hash"
        );

        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "old-password",
                "new-password"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.oldPassword(), user.getPasswordHash()))
                .thenReturn(true);
        when(passwordEncoder.encode(request.newPassword()))
                .thenReturn("test-new-password-hash");

        userService.updatePassword(1L, request);

        verify(passwordEncoder).matches("old-password", "test-password-hash");
        verify(passwordEncoder).encode("new-password");

        assertEquals("test-new-password-hash", user.getPasswordHash());
    }

    @Test
    void shouldRejectUpdatePasswordWhenPasswordWrong(){
        User user = User.create(
                "test-loginId",
                "test-password-hash"
        );

        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "wrong-password",
                "new-password"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "test-password-hash"))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updatePassword(1L, request)
        );

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        assertEquals("test-password-hash", user.getPasswordHash());

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("wrong-password", "test-password-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldRejectUpdatePasswordWhenUserNotFound(){
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "old-password",
                "new-password"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updatePassword(1L, request)
        );

        verifyNoInteractions(passwordEncoder);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldUpdateProfileWhenRequestIsValid(){
        UserProfile profile = UserProfile.create(
                1L,
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );

        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "update-last-name",
                "update-first-name",
                "111-111-1111",
                "update-test@email.com",
                "update-test-address"
        );

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.existsById(1L))
                .thenReturn(true);
        when(userProfileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        userService.updateProfile(1L, request);

        verify(userProfileRepository).save(captor.capture());
        UserProfile savedProfile = captor.getValue();
        assertAll(
                () -> assertEquals("update-last-name", savedProfile.getLastName()),
                () -> assertEquals("update-first-name", savedProfile.getFirstName()),
                () -> assertEquals("111-111-1111", savedProfile.getPhoneNumber()),
                () -> assertEquals("update-test@email.com", savedProfile.getEmail()),
                () -> assertEquals("update-test-address", savedProfile.getAddress())
        );
    }

    @Test
    void shouldRejectUpdateProfileWhenUserNotFound(){
        when(userRepository.existsById(1L))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfile(1L, null)
        );

        verify(userRepository).existsById(1L);
        verifyNoInteractions(userProfileRepository);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectUpdateProfileWhenProfileNotFound(){
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "update-last-name",
                "update-first-name",
                "111-111-1111",
                "update-test@email.com",
                "update-test-address"
        );

        when(userRepository.existsById(1L))
                .thenReturn(true);
        when(userProfileRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfile(1L, request)
        );

        verify(userRepository).existsById(1L);
        verify(userProfileRepository).findById(1L);
        verify(userProfileRepository, never()).save(any());
        assertEquals(ErrorCode.USER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldDeleteWhenRequestIsValid(){
        User user = User.create(
                "test-loginId",
                "test-password-hash"
        );

        UserPasswordRequest request = new UserPasswordRequest("password");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "test-password-hash"))
                .thenReturn(true);
        when(userProfileRepository.existsById(1L))
                .thenReturn(false);

        userService.delete(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("password", "test-password-hash");
        verify(userProfileRepository).existsById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void shouldRejectDeleteWhenUserNotFound(){
        UserPasswordRequest request = new UserPasswordRequest("password");

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.delete(1L, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userProfileRepository);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldRejectDeleteWhenWrongPassword() {
        User user = User.create(
                "test-loginId",
                "test-password-hash"
        );

        UserPasswordRequest request = new UserPasswordRequest("wrong-password");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "test-password-hash"))
                .thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.delete(1L, request)
        );

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        verify(passwordEncoder).matches("wrong-password", "test-password-hash");
        verify(userProfileRepository, never()).existsById(any());
        verify(userProfileRepository, never()).deleteById(any());
        verify(userRepository, never()).delete(any());
    }
}