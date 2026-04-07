# 🛒 Spring Boot Redis Cache — Product CRUD API

A production-style REST API demonstrating the **Cache-Aside pattern** using
**Redis as an L1 cache** and **MySQL as the primary data store (L2)**, built
with Spring Boot 4 and Java 21.

---

## Architecture

Client → Controller → Service → Redis Cache (L1)
↘ MySQL via JPA  (L2, on cache miss)

| Operation      | MySQL         | Redis                  |
|----------------|---------------|------------------------|
| Save           | ✅ Write       | ✅ SET with TTL         |
| Find All       | ✅ Always hit  | ❌ Not cached           |
| Find by ID     | ✅ On miss only| ✅ Cache-Aside           |
| Find by Name   | ✅ Always hit  | ❌ Not cached           |
| Update         | ✅ Write       | ✅ REFRESH with TTL     |
| Delete         | ✅ Delete      | ✅ EVICT                |

---

## Tech Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| Language      | Java 21                           |
| Framework     | Spring Boot 4                     |
| Cache         | Redis 7 via Jedis                 |
| Database      | MySQL 8                           |
| ORM           | Spring Data JPA / Hibernate       |
| Redis Client  | Jedis + RedisTemplate             |
| Build Tool    | Maven                             |
| Utilities     | Lombok, Spring Actuator           |

---

## Project Structure

redis-cache/
├── pom.xml
└── src/main/java/com/example/rediscache/
├── RedisCacheApplication.java
├── config/
│   └── RedisConfig.java          # Jedis, RedisTemplate, RedisCacheManager
├── entity/
│   └── Product.java              # JPA entity (id, name, qty, price)
├── repository/
│   └── ProductRepository.java    # JpaRepository + findByName()
├── service/
│   └── ProductService.java       # Cache-aside logic
└── controller/
└── ProductController.java    # REST endpoints


---

## API Endpoints

| Method   | Endpoint                          | Description         |
|----------|-----------------------------------|---------------------|
| `POST`   | `/api/v1/products`                | Create product      |
| `GET`    | `/api/v1/products`                | Get all products    |
| `GET`    | `/api/v1/products/{id}`           | Get by ID           |
| `GET`    | `/api/v1/products/search?name=X`  | Search by name      |
| `PUT`    | `/api/v1/products/{id}`           | Update product      |
| `DELETE` | `/api/v1/products/{id}`           | Delete product      |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+
- Redis 7+

### 1. Clone the repository
```bash
git clone https://github.com/your-username/spring-boot-redis-cache.git
cd spring-boot-redis-cache
```

### 2. Start MySQL and Redis
```bash
# MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=yourpassword \
  -e MYSQL_DATABASE=productdb \
  -p 3306:3306 mysql:8

# Redis
docker run -d --name redis \
  -p 6379:6379 redis:7-alpine
```

### 3. Configure `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/productdb
    username: root
    password: yourpassword

  data:
    redis:
      host: localhost
      port: 6379
      cache:
        ttl: 10        # Cache TTL in minutes
```

### 4. Run the application
```bash
./mvnw spring-boot:run
```

---

## Sample Request
```json
POST /api/v1/products
Content-Type: application/json

{
  "name": "iPhone 16",
  "qty": 50,
  "price": 999.99
}
```

---

## Cache Behaviour

GET /api/v1/products/6s7djbfh
First call  → CACHE MISS  → fetched from MySQL → stored in Redis (TTL: 10 min)
Second call → CACHE HIT   → returned from Redis → MySQL not touched
PUT/DELETE  → MySQL updated → Redis key refreshed or evicted

---

## Redis Key Pattern

Product:a4sdgegd   → cached Product with id = a4sdgegd
Product:6s7djbfh   → cached Product with id = 6s7djbfh

---

## Configuration

All Redis settings are externalised in `application.yml` — no hardcoded values in code.

| Property                        | Default     | Description              |
|---------------------------------|-------------|--------------------------|
| `spring.data.redis.host`        | `localhost` | Redis host               |
| `spring.data.redis.port`        | `6379`      | Redis port               |
| `spring.data.redis.cache.ttl`   | `10`        | Cache TTL in minutes     |
| `spring.datasource.url`         | —           | MySQL connection URL     |

---

## License

MIT License — free to use and modify.
