package com.onlinestore.order.service.api;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.onlinestore.order.dto.UpdateUserRequest;
import com.onlinestore.order.entity.User;

/**
 * Сервис для управления пользователями. Предоставляет CRUD операции и методы
 * для Spring Security.
 */
public interface UserService extends UserDetailsService {
	/**
	 * Находит пользователя по ID.
	 *
	 * @param userId
	 *            UUID пользователя
	 * @return сущность пользователя
	 * @throws UsernameNotFoundException
	 *             если пользователь не найден
	 */
	User getUserById(UUID userId);
	/**
	 * Возвращает список всех пользователей.
	 *
	 * @return список пользователей (может быть пустым)
	 */
	List<User> getAllUsers();
	/**
	 * Обновляет данные пользователя.
	 * <p>
	 * Обновляются только переданные поля. Проверяется уникальность username и email
	 * перед обновлением.
	 * </p>
	 *
	 * @param userId
	 *            ID пользователя
	 * @param request
	 *            данные для обновления (может содержать частичные поля)
	 * @return обновленный пользователь
	 * @throws UsernameNotFoundException
	 *             если пользователь не найден
	 * @throws IllegalArgumentException
	 *             если username или email уже заняты
	 */
	User updateUser(UUID userId, UpdateUserRequest request);
	/**
	 * Удаляет пользователя по ID.
	 *
	 * @param userId
	 *            ID пользователя
	 * @throws UsernameNotFoundException
	 *             если пользователь не найден
	 */
	void deleteUser(UUID userId);
}
