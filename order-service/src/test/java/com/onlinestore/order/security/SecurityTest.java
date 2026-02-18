package com.onlinestore.order.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.onlinestore.order.dto.LoginRequest;
import com.onlinestore.order.dto.RegisterRequest;
import com.onlinestore.order.entity.User;
import com.onlinestore.order.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void register_ValidRequest_ReturnsToken() throws Exception {
		// Arrange
		RegisterRequest request = new RegisterRequest("testuser", "test@email.com", "password123");

		// Act & Assert
		mockMvc.perform(MockMvcRequestBuilders.post("/auth/reg").contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(request))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.token").exists())
				.andExpect(MockMvcResultMatchers.jsonPath("$.username").value("testuser"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.role").value("ROLE_USER"));
	}

	@Test
	void login_ValidCredentials_ReturnsToken() throws Exception {
		// Arrange - создаем пользователя
		User user = User.builder().username("testuser").password(passwordEncoder.encode("password123"))
				.email("test@email.com").role(User.Role.ROLE_USER).build();
		userRepository.save(user);

		LoginRequest request = new LoginRequest("testuser", "password123");

		// Act & Assert
		mockMvc.perform(MockMvcRequestBuilders.post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(request))).andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	void adminEndpoint_WithAdminRole_AccessGranted() throws Exception {
		// Проверка доступа для админа
		mockMvc.perform(MockMvcRequestBuilders.get("/api/users")).andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	@WithMockUser(username = "user", roles = {"USER"})
	void adminEndpoint_WithUserRole_AccessDenied() throws Exception {
		// USER не может получить список пользователей
		mockMvc.perform(MockMvcRequestBuilders.get("/api/users"))
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}

	@Test
	void refreshToken_ValidToken_ReturnsNewToken() throws Exception {
		// 1. Регистрируем пользователя
		User user = User.builder().username("testuser").password(passwordEncoder.encode("password123"))
				.email("test@email.com").role(User.Role.ROLE_USER).build();
		userRepository.save(user);

		// 2. Логинимся для получения токена
		LoginRequest loginRequest = new LoginRequest("testuser", "password123");
		MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON).content(asJsonString(loginRequest))).andReturn();

		String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

		// 3. Обновляем токен
		mockMvc.perform(MockMvcRequestBuilders.post("/auth/refresh").header("Authorization", "Bearer " + token))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.token").exists());
	}

	private String asJsonString(Object obj) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(obj);
	}
}
