package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.common.error.BusinessException;
import io.github.blairjeon.stockflow.common.error.ErrorCode;
import io.github.blairjeon.stockflow.user.dto.*;
import io.github.blairjeon.stockflow.user.entity.User;
import io.github.blairjeon.stockflow.user.entity.UserProfile;
import io.github.blairjeon.stockflow.user.repository.UserProfileRepository;
import io.github.blairjeon.stockflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request){
        if(userRepository.existsByLoginId(request.loginId())){
            throw new BusinessException(ErrorCode.LOGIN_ID_TAKEN);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.loginId(), encodedPassword);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return UserResponse.from(savedUser);
        } catch (DataIntegrityViolationException e) {
            Throwable cause = e;

            while(cause!=null){
                if(cause instanceof ConstraintViolationException violation
                && "uk_users_login_id".equals(violation.getConstraintName())){
                    throw new BusinessException(ErrorCode.LOGIN_ID_TAKEN);
                }
                cause = cause.getCause();
            }
            throw e;
        }
    }

    @Transactional
    public void createProfile(Long userId, UserProfileCreateRequest request){
        if(!userRepository.existsById(userId)){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if(userProfileRepository.existsById(userId)){
            throw new BusinessException(ErrorCode.USER_PROFILE_ALREADY_EXISTS);
        }

        UserProfile profile = UserProfile.create(
                userId,
                request.lastName(),
                request.firstName(),
                request.phoneNumber(),
                request.email(),
                request.address()
        );

        userProfileRepository.save(profile);
    }

    public UserResponse getUser(Long userId){
        User user = getUserById(userId);
        return UserResponse.from(user);
    }

    public UserProfileResponse getProfile(Long userId){
        UserProfile profile = getProfileById(userId);
        return UserProfileResponse.from(profile);
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request){
        User user = getUserById(userId);

        if(!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedPassword);
    }

    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequest request){
        if(!userRepository.existsById(userId)){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if(request.isEmpty()){
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        UserProfile profile = getProfileById(userId);
        profile.updateProfile(
                request.lastName(),
                request.firstName(),
                request.phoneNumber(),
                request.email(),
                request.address()
        );

        userProfileRepository.save(profile);
    }

    @Transactional
    public void delete(Long userId, UserPasswordRequest request){
        User user = getUserById(userId);

        if(!passwordEncoder.matches(request.password(), user.getPasswordHash())){
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }

        if(userProfileRepository.existsById(userId)){
            userProfileRepository.deleteById(userId);
        }
        userRepository.delete(user);
    }

    private User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile getProfileById(Long userId){
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }
}