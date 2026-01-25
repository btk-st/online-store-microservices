package com.onlinestore.order.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinestore.order.dto.UpdateUserRequest;
import com.onlinestore.order.dto.UserResponse;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User adminUser;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .username("testuser")
                .email("test@email.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.ROLE_USER)
                .build());

        adminUser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@email.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ROLE_ADMIN)
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ============ GET CURRENT USER ============
    @Test
    void getCurrentUser_ReturnsUserData() throws Exception {
        authenticateAs(testUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void getCurrentUser_WhenUnauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    // ============ GET USER BY ID (ADMIN ONLY) ============
    @Test
    void getUserById_WhenAdmin_ReturnsUser() throws Exception {
        authenticateAs(adminUser);

        mockMvc.perform(get("/api/users/{userId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserById_WhenUser_ReturnsForbidden() throws Exception {
        authenticateAs(testUser);

        mockMvc.perform(get("/api/users/{userId}", adminUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_WhenUserNotFound_ReturnsForbidden() throws Exception {
        authenticateAs(adminUser);

        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/users/{userId}", nonExistentId))
                .andExpect(status().isForbidden());
    }

    // ============ GET ALL USERS (ADMIN ONLY) ============
    @Test
    void getAllUsers_WhenAdmin_ReturnsUsersList() throws Exception {
        authenticateAs(adminUser);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[1].username").value("admin"));
    }

    @Test
    void getAllUsers_WhenUser_ReturnsForbidden() throws Exception {
        authenticateAs(testUser);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ============ UPDATE CURRENT USER ============
    @Test
    void updateCurrentUser_ValidRequest_UpdatesUser() throws Exception {
        authenticateAs(testUser);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("updatedUser")
                .email("updated@email.com")
                .password("newPassword123")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updatedUser"))
                .andExpect(jsonPath("$.email").value("updated@email.com"));

        User updated = userRepository.findByUsername("updatedUser").orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();
    }

    @Test
    void updateCurrentUser_WithExistingUsername_ReturnsBadRequest() throws Exception {
        User anotherUser = userRepository.save(User.builder()
                .username("existingUser")
                .email("another@email.com")
                .password(passwordEncoder.encode("pass"))
                .role(User.Role.ROLE_USER)
                .build());

        authenticateAs(testUser);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("existingUser")
                .email("test@email.com")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCurrentUser_InvalidData_ReturnsBadRequest() throws Exception {
        authenticateAs(testUser);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("ab")
                .email("invalid-email")
                .password("123")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============ UPDATE OTHER USER (ADMIN ONLY) ============
    @Test
    void updateUser_WhenAdmin_UpdatesSuccessfully() throws Exception {
        authenticateAs(adminUser);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("adminUpdatedUser")
                .email("adminupdated@email.com")
                .role(User.Role.ROLE_ADMIN)
                .build();

        mockMvc.perform(put("/api/users/{userId}", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("adminUpdatedUser"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void updateUser_WhenUser_ReturnsForbidden() throws Exception {
        authenticateAs(testUser);

        UpdateUserRequest request = UpdateUserRequest.builder()
                .username("attempt")
                .email("attempt@email.com")
                .build();

        mockMvc.perform(put("/api/users/{userId}", adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ============ DELETE CURRENT USER ============
    @Test
    void deleteCurrentUser_DeletesUser() throws Exception {
        authenticateAs(testUser);

        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByUsername("testuser")).isEmpty();
    }

    // ============ DELETE OTHER USER (ADMIN ONLY) ============
    @Test
    void deleteUser_WhenAdmin_DeletesUser() throws Exception {
        authenticateAs(adminUser);

        mockMvc.perform(delete("/api/users/{userId}", testUser.getId()))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(testUser.getId())).isEmpty();
    }

    @Test
    void deleteUser_WhenUser_ReturnsForbidden() throws Exception {
        authenticateAs(testUser);

        mockMvc.perform(delete("/api/users/{userId}", adminUser.getId()))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(adminUser.getId())).isPresent();
    }

    @Test
    void deleteUser_NonExistentUser_ReturnsNotFound() throws Exception {
        authenticateAs(adminUser);

        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(delete("/api/users/{userId}", nonExistentId))
                .andExpect(status().isForbidden());
    }

}
