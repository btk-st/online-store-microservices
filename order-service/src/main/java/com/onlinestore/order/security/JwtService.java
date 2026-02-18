package com.onlinestore.order.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

/**
 * Сервис для работы с JWT токенами.
 * <p>
 * Отвечает за генерацию, парсинг и валидацию JWT токенов. Использует
 * конфигурацию из {@link JwtProperties} для настройки:
 * <ul>
 * <li>Секретного ключа для подписи</li>
 * <li>Времени жизни токена (expiration)</li>
 * </ul>
 * </p>
 *
 * <h3>Структура токена:</h3>
 * <ul>
 * <li><b>subject</b> — username пользователя</li>
 * <li><b>issuedAt</b> — время создания</li>
 * <li><b>expiration</b> — время истечения</li>
 * <li><b>claims</b> — дополнительные данные (роли и т.д.)</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see JwtProperties
 */
@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtProperties jwtProperties;

	/**
	 * Создает секретный ключ для подписи JWT из секрета в конфигурации.
	 *
	 * @return SecretKey для подписи/валидации токенов
	 */
	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
	}

	/**
	 * Генерирует JWT токен для пользователя (без дополнительных claims).
	 *
	 * @param userDetails
	 *            данные пользователя
	 * @return JWT токен в формате строки
	 */
	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	/**
	 * Генерирует JWT токен с дополнительными claims.
	 *
	 * @param extraClaims
	 *            дополнительные данные для включения в токен
	 * @param userDetails
	 *            данные пользователя
	 * @return JWT токен в формате строки
	 */
	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return Jwts.builder().claims(extraClaims).subject(userDetails.getUsername())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
				.signWith(getSigningKey()).compact();
	}

	/**
	 * Проверяет валидность JWT токена для указанного пользователя. Токен валиден,
	 * если:
	 * <ul>
	 * <li>username в токене совпадает с username пользователя</li>
	 * <li>токен не истек</li>
	 * </ul>
	 *
	 * @param token
	 *            JWT токен
	 * @param userDetails
	 *            данные пользователя для проверки
	 * @return true если токен валиден
	 */
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	/**
	 * Извлекает username из JWT токена.
	 *
	 * @param token
	 *            JWT токен
	 * @return username пользователя (subject токена)
	 */
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Проверяет, истек ли токен.
	 *
	 * @param token
	 *            JWT токен
	 * @return true если срок действия истек
	 */
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	/**
	 * Извлекает дату истечения из токена.
	 *
	 * @param token
	 *            JWT токен
	 * @return дата истечения
	 */
	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	/**
	 * Извлекает конкретный claim из токена.
	 *
	 * @param token
	 *            JWT токен
	 * @param claimsResolver
	 *            функция для извлечения нужного поля из Claims
	 * @return запрошенное значение
	 */
	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Парсит токен и извлекает все claims. Проверяет подпись токена с
	 * использованием секретного ключа.
	 *
	 * @param token
	 *            JWT токен
	 * @return все claims из токена
	 */
	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
	}
}
