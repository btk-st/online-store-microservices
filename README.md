# 🛍️ Система обработки заказов

Микросервисная система для интернет-магазина на Spring Boot.

## 🚀 Быстрый запуск

1. **Собрать проекты:**
```bash
mvn clean install -f order-service/pom.xml
mvn clean install -f inventory-service/pom.xml
mvn clean install -f notification-service/pom.xml
```

2. **Запустить всю систему:**
```bash
docker-compose up -d
```

Готово! Все сервисы запустятся автоматически.

## 🌐 Доступные сервисы

| Сервис | Порт | Назначение |
|--------|------|------------|
| **Order Service** | 8082 | Заказы и авторизация |
| **Inventory Service** | 8081 | Управление товарами |
| **Notification Service** | 8083 | Аналитика заказов |
| **Kafka UI** | 8090 | Мониторинг Kafka |
| **Redis** | 6379 | Кэширование |

## 📖 Документация API (Swagger)

После запуска системы документация API доступна по адресам:

| Сервис | Swagger UI | OpenAPI JSON |
|--------|------------|--------------|
| **Order Service** | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| **Inventory Service** | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| **Notification Service** | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

**Swagger UI предоставляет:**
- Полный список всех endpoint'ов
- Возможность тестирования API прямо из браузера
- Описание моделей данных
- Информацию о требуемых заголовках (JWT и т.д.)

## 🛠️ Управление системой

```bash
# Остановить систему
docker-compose down

# Остановить и удалить данные
docker-compose down -v

# Перезапустить один сервис
docker-compose restart order-service

# Посмотреть логи
docker-compose logs -f

# Проверить статус
docker-compose ps
```

## 📁 Структура проекта
```
├── order-service/       # (порт 8082)
├── inventory-service/   # (порт 8081)
├── notification-service/# (порт 8083)
└── docker-compose.yml   # Запуск всего стека
```

**Запускается:** 3 микросервиса + 3 PostgreSQL + Kafka + Zookeeper + Redis + Kafka UI