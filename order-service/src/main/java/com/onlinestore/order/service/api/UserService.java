package com.onlinestore.order.service.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.onlinestore.order.dto.UpdateUserRequest;
import com.onlinestore.order.entity.User;

public interface UserService extends UserDetailsService {
	User getUserById(UUID userId);
	List<User> getAllUsers();
	User updateUser(UUID userId, UpdateUserRequest request);
	void deleteUser(UUID userId);
}
