package io.github.blairjeon.stockflow.user;

import io.github.blairjeon.stockflow.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(
            @Valid @RequestBody UserCreateRequest request
    ){
        return userService.create(request);
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    public void createProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileCreateRequest request
    ){
        userService.createProfile(userId, request);
    }

    @GetMapping
    public UserResponse get(@AuthenticationPrincipal Long userId){
        return userService.getUser(userId);
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal Long userId){
        return userService.getProfile(userId);
    }

    @PatchMapping("/password")
    public void updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ) {
        userService.updatePassword(userId, request);
    }

    @PatchMapping("/profile")
    public void updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserProfileUpdateRequest request
    ){
        userService.updateProfile(userId, request);
    }

    @DeleteMapping("/delete")
    public void delete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserPasswordRequest request
    ){
        userService.delete(userId, request);
    }
}