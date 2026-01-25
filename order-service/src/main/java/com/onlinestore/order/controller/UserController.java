package com.onlinestore.order.controller;

import com.onlinestore.order.controller.api.UserApi;
import com.onlinestore.order.dto.UpdateUserRequest;
import com.onlinestore.order.dto.UserResponse;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @GetMapping("/me")
    @Override
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal User currentUser) {

        UserResponse response = mapToResponse(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    //TODO: не работают роли
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        UserResponse response = mapToResponse(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/me")
    @Override
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequest request) {

        User updatedUser = userService.updateUser(currentUser.getId(), request);
        UserResponse response = mapToResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {

        User updatedUser = userService.updateUser(userId, request);
        UserResponse response = mapToResponse(updatedUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @Override
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticationPrincipal User currentUser) {

        userService.deleteUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
