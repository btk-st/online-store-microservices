package com.onlinestore.order.dto;

import java.util.UUID;

import com.onlinestore.order.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
	private UUID id;
	private String username;
	private String email;
	private User.Role role;
}
