package com.onlinestore.order.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Конфигурация безопасности приложения.
 * <p>
 * Настраивает:
 * <ul>
 * <li>Цепочку фильтров безопасности (SecurityFilterChain)</li>
 * <li>JWT аутентификацию через {@link JwtAuthenticationFilter}</li>
 * <li>Stateless сессии</li>
 * <li>Провайдер аутентификации</li>
 * <li>Публичные эндпоинты (доступные без токена)</li>
 * </ul>
 * </p>
 *
 * <h3>Публичные эндпоинты (permitAll):</h3>
 * <ul>
 * <li><b>/auth/**</b> - регистрация, логин, refresh токена</li> *
 * <li><b>/swagger-ui/**</b> - Swagger UI документация</li>
 * <li><b>/v3/api-docs/**</b> - OpenAPI спецификация</li>
 * </ul>
 *
 * <h3>Защищенные эндпоинты:</h3>
 * <ul>
 * <li>Все остальные запросы требуют валидный JWT токен</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see JwtService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter;
	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Настраивает цепочку фильтров безопасности.
	 * <p>
	 * Порядок работы:
	 * <ol>
	 * <li>JwtAuthenticationFilter проверяет JWT токен (ДО стандартного фильтра
	 * аутентификации)</li>
	 * <li>При успешной аутентификации устанавливает SecurityContext</li>
	 * <li>Проверяются права доступа к эндпоинту</li>
	 * </ol>
	 * </p>
	 *
	 * @param http
	 *            конфигуратор HTTP безопасности
	 * @return построенная цепочка фильтров
	 * @throws Exception
	 *             если ошибка конфигурации
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll().anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Создает провайдер аутентификации для проверки учетных данных. Использует
	 * {@link UserDetailsService} для загрузки пользователей и
	 * {@link PasswordEncoder} для проверки паролей.
	 *
	 * @return настроенный DaoAuthenticationProvider
	 */
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder);
		return authProvider;
	}

	/**
	 * Предоставляет менеджер аутентификации для использования в контроллерах.
	 *
	 * @param config
	 *            конфигурация аутентификации Spring
	 * @return AuthenticationManager
	 * @throws Exception
	 *             если ошибка получения менеджера
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
