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
    public void create(@Valid @RequestBody UserCreateRequest request){
        userService.create(request);
    }

    @GetMapping("/me")
    public UserResponse get(@AuthenticationPrincipal Long userId){
        return userService.get(userId);
    }

    @PatchMapping("/me/password")
    public void updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserPasswordUpdateRequest request
    ){
        userService.updatePassword(userId, request);
    }

    @PatchMapping("/me/name")
    public void updateDisplayName(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDisplayNameUpdateRequest request
    ){
        userService.updateDisplayName(userId, request);
    }

    @DeleteMapping("/me")
    public void delete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserDeleteRequest request
    ){
        userService.delete(userId, request);
    }
}