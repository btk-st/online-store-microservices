package com.onlinestore.order.service.api;

import com.onlinestore.order.dto.RegisterRequest;
import com.onlinestore.order.entity.User;

public interface AuthService {
	User register(RegisterRequest request);
}
