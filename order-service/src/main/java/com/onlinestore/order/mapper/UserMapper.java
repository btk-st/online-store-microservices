package com.onlinestore.order.mapper;

import org.springframework.stereotype.Component;

import com.onlinestore.order.dto.RegisterRequest;
import com.onlinestore.order.entity.User;

@Component
public class UserMapper {
	public User toUser(RegisterRequest request) {
		return User.builder().username(request.getUsername()).email(request.getEmail()).role(User.Role.ROLE_USER)
				.build();
	}
}
