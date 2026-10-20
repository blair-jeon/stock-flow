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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    private static final String LOGIN_ID = "integration-user";
    private static final String PASSWORD = "old-password123!";

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        userProfileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldCreateAndGetUserWithEncodedPassword() {
        UserResponse response = userService.create(new UserCreateRequest(LOGIN_ID, PASSWORD));

        User savedUser = userRepository.findByLoginId(LOGIN_ID).orElseThrow();
        assertAll(
                () -> assertTrue(savedUser.getId() > 0),
                () -> assertEquals(LOGIN_ID, savedUser.getLoginId()),
                () -> assertNotEquals(PASSWORD, savedUser.getPasswordHash()),
                () -> assertTrue(passwordEncoder.matches(PASSWORD, savedUser.getPasswordHash())),
                () -> Assertions.assertEquals(UserRole.USER, savedUser.getRole()),
                () -> Assertions.assertEquals(UserStatus.ACTIVE, savedUser.getStatus()),
                () -> assertNotNull(savedUser.getCreatedAt()),
                () -> assertNotNull(savedUser.getUpdatedAt()),
                () -> assertEquals(new UserResponse(LOGIN_ID, UserRole.USER, UserStatus.ACTIVE), response),
                () -> assertEquals(response, userService.getUser(savedUser.getId()))
        );
    }

    @Test
    void shouldRejectDuplicateLoginIdWithoutChangingExistingUser() {
        User existingUser = createUser();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.create(new UserCreateRequest(LOGIN_ID, "different-password"))
        );

        User savedUser = userRepository.findById(existingUser.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(ErrorCode.LOGIN_ID_TAKEN, exception.getErrorCode()),
                () -> assertEquals(1L, userRepository.count()),
                () -> assertEquals(existingUser.getPasswordHash(), savedUser.getPasswordHash())
        );
    }

    @Test
    void shouldCreateAndGetProfile() {
        User user = createUser();

        userService.createProfile(user.getId(), profileRequest());
        UserProfile profile = userProfileRepository.findById(user.getId())
                .orElseThrow();

        assertAll(
                () -> assertEquals(user.getId(), profile.getUserId()),
                () -> assertEquals("test-last-name", profile.getLastName()),
                () -> assertEquals("test-first-name", profile.getFirstName()),
                () -> assertEquals("000-000-0000", profile.getPhoneNumber()),
                () -> assertEquals("test@example.com", profile.getEmail()),
                () -> assertEquals("test-address", profile.getAddress()),
                () -> assertNotNull(profile.getCreatedAt()),
                () -> assertEquals(new UserProfileResponse("test-last-name", "test-first-name", "000-000-0000",
                        "test@example.com", "test-address"), userService.getProfile(user.getId()))
        );
    }

    @Test
    void shouldRejectProfileForMissingUser() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createProfile(Long.MAX_VALUE, profileRequest()));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        assertEquals(0L, userProfileRepository.count());
    }

    @Test
    void shouldRejectDuplicateProfileWithoutChangingExistingProfile() {
        User user = createUserWithProfile();
        UserProfileResponse before = userService.getProfile(user.getId());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.createProfile(user.getId(), new UserProfileCreateRequest(
                        "test-new-last-name", "test-new-first-name", "000-111-2222", "test-new@example.com", "test-new-address")));

        assertEquals(ErrorCode.USER_PROFILE_ALREADY_EXISTS, exception.getErrorCode());
        assertEquals(1L, userProfileRepository.count());
        assertEquals(before, userService.getProfile(user.getId()));
    }

    @Test
    void shouldPersistPasswordChangeThroughDirtyChecking() {
        User user = createUser();
        String newPassword = "new-password123!";

        userService.updatePassword(user.getId(), new UserPasswordUpdateRequest(PASSWORD, newPassword));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertAll(
                () -> assertNotEquals(user.getPasswordHash(), updatedUser.getPasswordHash()),
                () -> assertNotEquals(newPassword, updatedUser.getPasswordHash()),
                () -> assertTrue(passwordEncoder.matches(newPassword, updatedUser.getPasswordHash())),
                () -> assertFalse(passwordEncoder.matches(PASSWORD, updatedUser.getPasswordHash()))
        );
    }

    @Test
    void shouldKeepPasswordWhenOldPasswordIsWrong() {
        User user = createUser();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updatePassword(user.getId(),
                        new UserPasswordUpdateRequest("wrong-password", "new-password123!")));

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        assertEquals(user.getPasswordHash(),
                userRepository.findById(user.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void shouldPersistPartialProfileUpdateAndPreserveOtherFields() {
        User user = createUserWithProfile();

        userService.updateProfile(user.getId(),
                new UserProfileUpdateRequest(null, null, null, "test-update@example.com", "test-update-address"));

        UserProfile profile = userProfileRepository.findById(user.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("test-last-name", profile.getLastName()),
                () -> assertEquals("test-first-name", profile.getFirstName()),
                () -> assertEquals("000-000-0000", profile.getPhoneNumber()),
                () -> assertEquals("test-update@example.com", profile.getEmail()),
                () -> assertEquals("test-update-address", profile.getAddress())
        );
    }

    @Test
    void shouldRejectEmptyProfileUpdateWithoutChangingProfile() {
        User user = createUserWithProfile();
        UserProfileResponse before = userService.getProfile(user.getId());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateProfile(user.getId(),
                        new UserProfileUpdateRequest(null, null, null, null, null)));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals(before, userService.getProfile(user.getId()));
    }

    @Test
    void shouldDeleteUserAndProfile() {
        User user = createUserWithProfile();

        userService.delete(user.getId(), new UserPasswordRequest(PASSWORD));

        assertFalse(userRepository.existsById(user.getId()));
        assertFalse(userProfileRepository.existsById(user.getId()));
    }

    @Test
    void shouldDeleteUserWithoutProfile() {
        User user = createUser();

        userService.delete(user.getId(), new UserPasswordRequest(PASSWORD));

        assertFalse(userRepository.existsById(user.getId()));
        assertEquals(0L, userProfileRepository.count());
    }

    @Test
    void shouldKeepUserAndProfileWhenDeletePasswordIsWrong() {
        User user = createUserWithProfile();
        UserProfileResponse before = userService.getProfile(user.getId());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.delete(user.getId(), new UserPasswordRequest("wrong-password")));

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
        assertTrue(userRepository.existsById(user.getId()));
        assertEquals(before, userService.getProfile(user.getId()));
    }

    @Test
    void shouldRejectGetUserWhenUserIsMissing() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getUser(Long.MAX_VALUE));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void shouldRejectGetProfileWhenProfileIsMissing() {
        User user = createUser();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.getProfile(user.getId()));

        assertEquals(ErrorCode.USER_PROFILE_NOT_FOUND, exception.getErrorCode());
    }

    private User createUser() {
        userService.create(new UserCreateRequest(LOGIN_ID, PASSWORD));
        return userRepository.findByLoginId(LOGIN_ID).orElseThrow();
    }

    private User createUserWithProfile() {
        User user = createUser();
        userService.createProfile(user.getId(), profileRequest());
        return user;
    }

    private UserProfileCreateRequest profileRequest() {
        return new UserProfileCreateRequest(
                "test-last-name",
                "test-first-name",
                "000-000-0000",
                "test@example.com",
                "test-address");
    }
}
