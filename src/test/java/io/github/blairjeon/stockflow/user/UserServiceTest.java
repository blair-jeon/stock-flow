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
    private static final long USER_ID = 1L;
    private static final long MISSING_USER_ID = 2L;
    private static final String LOGIN_ID = "test-login-id";
    private static final String PASSWORD = "test-password";
    private static final String PASSWORD_HASH = "test-password-hash";

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private User existingUser;
    private UserProfile existingProfile;

    @BeforeEach
    void setUp() {
        existingUser = User.create(LOGIN_ID, PASSWORD_HASH);

        existingProfile = UserProfile.create(
                USER_ID,
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@email.com",
                "test-address"
        );
    }

    @Test
    void shouldCreateUserWithEncodedPassword() {
        UserCreateRequest request = new UserCreateRequest(LOGIN_ID, PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByLoginId(LOGIN_ID)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        verify(userRepository).saveAndFlush(captor.capture());
        User savedUser = captor.getValue();

        assertAll(
                () -> assertEquals(LOGIN_ID, savedUser.getLoginId()),
                () -> assertEquals(PASSWORD_HASH, savedUser.getPasswordHash()),
                () -> assertEquals(UserRole.USER, savedUser.getRole()),
                () -> assertEquals(UserStatus.ACTIVE, savedUser.getStatus()),
                () -> assertEquals(new UserResponse(LOGIN_ID, UserRole.USER, UserStatus.ACTIVE), response)
        );
    }

    @Test
    void shouldRejectUserCreationWhenLoginIdAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(LOGIN_ID, PASSWORD);

        when(userRepository.existsByLoginId(LOGIN_ID)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.create(request));

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any());
        assertEquals(ErrorCode.LOGIN_ID_TAKEN, exception.getErrorCode());
    }

    @Test
    void shouldRejectUserCreationWhenLoginIdUniqueConstraintIsViolated() {
        UserCreateRequest request = new UserCreateRequest(LOGIN_ID, PASSWORD);

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate login ID",
                new SQLException("Duplicate entry"),
                "uk_users_login_id"
        );

        DataIntegrityViolationException dbException = new DataIntegrityViolationException("Insert failed", cause);

        when(userRepository.existsByLoginId(LOGIN_ID)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(dbException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.create(request)
        );

        verify(userRepository).saveAndFlush(any(User.class));
        assertEquals(ErrorCode.LOGIN_ID_TAKEN, exception.getErrorCode());
    }

    @Test
    void shouldPropagateUnrelatedDatabaseConstraintViolations() {
        ConstraintViolationException cause = new ConstraintViolationException(
                "Other constraint violated", new SQLException("Constraint violation"), "other_constraint");
        DataIntegrityViolationException dbException = new DataIntegrityViolationException("Insert failed", cause);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(dbException);

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> userService.create(new UserCreateRequest(LOGIN_ID, PASSWORD)));

        assertSame(dbException, exception);
    }

    @Test
    void shouldCreateProfileWhenUserExistsAndHasNoProfile() {
        UserProfileCreateRequest request = validProfileCreateRequest();

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userProfileRepository.existsById(USER_ID)).thenReturn(false);

        userService.createProfile(USER_ID, request);

        verify(userProfileRepository).save(captor.capture());
        UserProfile savedProfile = captor.getValue();

        assertAll(
                () -> assertEquals(USER_ID, savedProfile.getUserId()),
                () -> assertEquals("test-request-last-name", savedProfile.getLastName()),
                () -> assertEquals("test-request-first-name", savedProfile.getFirstName()),
                () -> assertEquals("111-111-1111", savedProfile.getPhoneNumber()),
                () -> assertEquals("test-request@email.com", savedProfile.getEmail()),
                () -> assertEquals("test-request-address", savedProfile.getAddress())
        );
    }

    @Test
    void shouldRejectProfileCreationWhenUserDoesNotExist() {
        UserProfileCreateRequest request = validProfileCreateRequest();

        when(userRepository.existsById(MISSING_USER_ID)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createProfile(MISSING_USER_ID, request)
        );

        verifyNoInteractions(userProfileRepository);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectProfileCreationWhenProfileAlreadyExists() {
        UserProfileCreateRequest request = validProfileCreateRequest();

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userProfileRepository.existsById(USER_ID)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createProfile(USER_ID, request)
        );

        assertEquals(ErrorCode.USER_PROFILE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void shouldReturnUserWhenUserExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUser(USER_ID);

        assertAll(
                () -> assertEquals(LOGIN_ID, response.loginId()),
                () -> assertEquals(UserRole.USER, response.role()),
                () -> assertEquals(UserStatus.ACTIVE, response.status())
        );
    }

    @Test
    void shouldThrowUserNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getUser(MISSING_USER_ID));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldReturnProfileWhenProfileExists() {
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(existingProfile));

        UserProfileResponse response = userService.getProfile(USER_ID);

        assertAll(
                () -> assertEquals("test-last-name", response.lastName()),
                () -> assertEquals("test-first-name", response.firstName()),
                () -> assertEquals("000-000-0000", response.phoneNumber()),
                () -> assertEquals("test@email.com", response.email()),
                () -> assertEquals("test-address", response.address())
        );
    }

    @Test
    void shouldThrowProfileNotFoundWhenProfileDoesNotExist() {
        when(userProfileRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.getProfile(MISSING_USER_ID)
        );

        assertEquals(ErrorCode.USER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldUpdatePasswordWhenCurrentPasswordIsCorrect() {
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                PASSWORD,
                "new-password"
        );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, existingUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("test-new-password-hash");

        userService.updatePassword(USER_ID, request);

        assertEquals("test-new-password-hash", existingUser.getPasswordHash());
    }

    @Test
    void shouldRejectPasswordUpdateWhenCurrentPasswordIsIncorrect() {
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                "wrong-password",
                "new-password"
        );

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updatePassword(USER_ID, request)
        );

        verify(passwordEncoder, never()).encode(any());

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        assertEquals(PASSWORD_HASH, existingUser.getPasswordHash());
    }

    @Test
    void shouldRejectPasswordUpdateWhenUserDoesNotExist() {
        UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(
                PASSWORD,
                "new-password"
        );

        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updatePassword(MISSING_USER_ID, request)
        );

        verifyNoInteractions(passwordEncoder);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldUpdateAllProfileFields() {
        UserProfileUpdateRequest request = validProfileUpdateRequest();

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(existingProfile));

        userService.updateProfile(USER_ID, request);

        verify(userProfileRepository).save(existingProfile);

        assertAll(
                () -> assertEquals("test-update-last-name", existingProfile.getLastName()),
                () -> assertEquals("test-update-first-name", existingProfile.getFirstName()),
                () -> assertEquals("111-111-1111", existingProfile.getPhoneNumber()),
                () -> assertEquals("test-update@email.com", existingProfile.getEmail()),
                () -> assertEquals("test-update-address", existingProfile.getAddress())
        );
    }

    @Test
    void shouldPreserveUnspecifiedFieldsWhenUpdatingProfile() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.of(existingProfile));
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, null, "updated@email.com", null);

        userService.updateProfile(USER_ID, request);

        verify(userProfileRepository).save(existingProfile);
        assertAll(
                () -> assertEquals("test-last-name", existingProfile.getLastName()),
                () -> assertEquals("test-first-name", existingProfile.getFirstName()),
                () -> assertEquals("000-000-0000", existingProfile.getPhoneNumber()),
                () -> assertEquals("updated@email.com", existingProfile.getEmail()),
                () -> assertEquals("test-address", existingProfile.getAddress())
        );
    }

    @Test
    void shouldRejectProfileUpdateWhenUserDoesNotExist() {
        UserProfileUpdateRequest request = validProfileUpdateRequest();

        when(userRepository.existsById(MISSING_USER_ID)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfile(MISSING_USER_ID, request)
        );

        verifyNoInteractions(userProfileRepository);
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectProfileUpdateWhenProfileDoesNotExist() {
        UserProfileUpdateRequest request = validProfileUpdateRequest();

        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userProfileRepository.findById(USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfile(USER_ID, request)
        );

        verify(userProfileRepository, never()).save(any());
        assertEquals(ErrorCode.USER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectProfileUpdateWhenNoFieldsAreProvided() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                null, null, null, null, null
        );

        when(userRepository.existsById(USER_ID)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateProfile(USER_ID, request)
        );

        verifyNoInteractions(userProfileRepository);

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    void shouldDeleteUserWhenUserHasNoProfile() {
        UserPasswordRequest request = new UserPasswordRequest(PASSWORD);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userProfileRepository.existsById(USER_ID)).thenReturn(false);

        userService.delete(USER_ID, request);

        verify(userRepository).delete(existingUser);
        verify(userProfileRepository, never()).deleteById(any());
    }

    @Test
    void shouldDeleteUserAndProfileWhenUserHasProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userProfileRepository.existsById(USER_ID)).thenReturn(true);

        userService.delete(USER_ID, new UserPasswordRequest(PASSWORD));

        verify(userProfileRepository).deleteById(USER_ID);
        verify(userRepository).delete(existingUser);
    }

    @Test
    void shouldRejectUserDeletionWhenUserDoesNotExist() {
        UserPasswordRequest request = new UserPasswordRequest(PASSWORD);

        when(userRepository.findById(MISSING_USER_ID)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.delete(MISSING_USER_ID, request)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(userProfileRepository);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void shouldRejectUserDeletionWhenPasswordIsIncorrect() {
        UserPasswordRequest request = new UserPasswordRequest("wrong-password");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.delete(USER_ID, request)
        );

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        verifyNoInteractions(userProfileRepository);
        verify(userRepository, never()).delete(any());
    }

    private UserProfileCreateRequest validProfileCreateRequest() {
        return new UserProfileCreateRequest(
                "test-request-last-name", "test-request-first-name", "111-111-1111",
                "test-request@email.com", "test-request-address");
    }

    private UserProfileUpdateRequest validProfileUpdateRequest() {
        return new UserProfileUpdateRequest(
                "test-update-last-name", "test-update-first-name", "111-111-1111",
                "test-update@email.com", "test-update-address");
    }
}
