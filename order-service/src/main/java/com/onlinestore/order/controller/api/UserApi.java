package com.onlinestore.order.controller.api;

import com.onlinestore.order.dto.UpdateUserRequest;
import com.onlinestore.order.dto.UserResponse;
import com.onlinestore.order.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users", description = "User management API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/users")
public interface UserApi {

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser);

    @Operation(summary = "Get user by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}")
    ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId);

    @Operation(summary = "Get all users (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    ResponseEntity<List<UserResponse>> getAllUsers();

    @Operation(summary = "Update current user profile")
    @PutMapping("/me")
    ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequest request);

    @Operation(summary = "Update any user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}")
    ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request);

    @Operation(summary = "Delete current user account")
    @DeleteMapping("/me")
    ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal User currentUser);

    @Operation(summary = "Delete user (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "User UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId);
}
