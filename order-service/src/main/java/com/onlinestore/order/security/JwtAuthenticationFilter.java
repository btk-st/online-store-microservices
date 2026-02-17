package com.onlinestore.order.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

/**
 * Фильтр аутентификации для JWT токенов.
 * <p>
 * Обрабатывает входящие запросы, извлекает JWT из заголовка Authorization,
 * валидирует токен и устанавливает аутентификацию в SecurityContext.
 * </p>
 *
 * <h3>Логика работы:</h3>
 * <ol>
 * <li>Проверяет наличие заголовка "Authorization: Bearer {token}"</li>
 * <li>Если заголовок отсутствует или неверный - пропускает запрос дальше (401
 * вернет Spring Security)</li>
 * <li>Извлекает username из токена через {@link JwtService}</li>
 * <li>Загружает UserDetails по username</li>
 * <li>Валидирует токен через {@link JwtService}</li>
 * <li>Устанавливает аутентификацию в SecurityContext</li>
 * </ol>
 *
 * <h3>Ожидаемый заголовок:</h3>
 * 
 * <pre>
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * </pre>
 *
 * @see JwtService
 * @see org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	/**
	 * Фильтрует запрос, выполняя JWT аутентификацию.
	 *
	 * @param request
	 *            HTTP запрос
	 * @param response
	 *            HTTP ответ
	 * @param filterChain
	 *            цепочка фильтров
	 */
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);
		final String username = jwtService.extractUsername(jwt);

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}

		filterChain.doFilter(request, response);
	}
}
