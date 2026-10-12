package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.user.dto.*;
import io.github.blairjeon.stockflow.user.exception.DuplicateLoginIdException;
import io.github.blairjeon.stockflow.user.exception.PasswordMismatchException;
import io.github.blairjeon.stockflow.user.exception.UserNotFoundException;
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
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void create(UserCreateRequest request){
        if(userRepository.existsByLoginId(request.login())){
            throw new DuplicateLoginIdException();
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.create(request.login(), encodedPassword, request.displayName());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if(e.getCause() instanceof ConstraintViolationException cve)
                if("uk_users_loginId".equals(cve.getConstraintName())){
                    throw new DuplicateLoginIdException();
                }
            throw e;
        }
    }

    public UserResponse get(Long userId){
        return UserResponse.from(getUserById(userId));
    }

    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request){
        User user = getUserById(userId);
        verifyPassword(request.oldPassword(), user.getPasswordHash());

        String newPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(newPassword);
    }

    @Transactional
    public void updateDisplayName(Long userId, UserDisplayNameUpdateRequest request){
        User user = getUserById(userId);
        verifyPassword(request.password(), user.getPasswordHash());

        user.updateDisplayName(request.displayName());
    }

    @Transactional
    public void delete(Long userId, UserDeleteRequest request){
        User user = getUserById(userId);
        verifyPassword(request.password(), user.getPasswordHash());

        userRepository.delete(user);
    }

    private User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private void verifyPassword(String rawPassword, String encodedPassword){
        if(!passwordEncoder.matches(rawPassword, encodedPassword)){
            throw new PasswordMismatchException();
        }
    }
}