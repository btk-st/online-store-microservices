package com.onlinestore.order.service.api;

import com.onlinestore.order.dto.RegisterRequest;
import com.onlinestore.order.entity.User;

/**
 * Сервис аутентификации и регистрации пользователей. Предоставляет методы для
 * создания новых учетных записей.
 */
public interface AuthService {
	/**
	 * Регистрирует нового пользователя в системе.
	 * <p>
	 * Перед созданием проверяет уникальность username и email. Пароль автоматически
	 * хешируется перед сохранением.
	 * </p>
	 *
	 * @param request
	 *            данные для регистрации (username, email, password)
	 * @return созданный пользователь с присвоенным ID
	 * @throws IllegalArgumentException
	 *             если:
	 *             <ul>
	 *             <li>username уже существует</li>
	 *             <li>email уже существует</li>
	 *             </ul>
	 */
	User register(RegisterRequest request);
}
