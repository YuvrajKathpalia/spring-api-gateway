# microgateway

A small, production-style **API Gateway** built with **Spring Cloud Gateway (WebFlux)** in front of two backend services. The gateway handles routing and JWT authentication; the backend services stay focused on business logic and trust the identity the gateway forwards.

Built as a focused learning/portfolio project — small enough to understand end to end, realistic enough to reflect real production gateway patterns.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Gateway | Spring Cloud Gateway 2024.0.0 (reactive / WebFlux) |
| Backend services | Spring Web MVC + Spring Data JPA |
| Auth | JWT (JJWT 0.12.6, HS256) |
| Database | PostgreSQL (one DB per service) |
| Password hashing | BCrypt |
| API docs | springdoc-openapi (Swagger UI) |
| Build & run | Maven, Docker, Docker Compose |

---

## Architecture

```
                 ┌──────────────┐
   Client  ───▶  │   Gateway    │   :8080  (reactive / WebFlux)
                 │  - routing   │
                 │  - JWT verify│
                 │  - logging   │
                 └──────┬───────┘
                        │ injects X-User-Id / X-User-Email / X-User-Roles
            ┌───────────┴───────────┐
            ▼                       ▼
   ┌─────────────────┐     ┌────────────────────┐
   │  User Service   │     │  Product Service   │
   │  :8081          │     │  :8082             │
   │  register/login │     │  product CRUD      │
   │  (JWT issuer)   │     │  (trusts headers)  │
   └────────┬────────┘     └─────────┬──────────┘
            ▼                        ▼
        userdb                   productdb        (PostgreSQL :5432)
```

**Responsibility split (the core idea):**

| Role | Who | What |
|------|-----|------|
| **Issuer** | user-service | signs the JWT at login |
| **Verifier** | gateway | validates the JWT, injects identity headers |
| **Truster** | product-service | trusts `X-User-*` headers, never parses a JWT |

The gateway is the single security boundary. Backend services never see the token.

---

## Request flow (login → protected call)

```
1. POST /auth/login  ───▶  Gateway  ───▶  user-service
                                            verifies password (BCrypt)
                                            SIGNS a JWT { userId, email, roles }
   ◀── { token } ──────────────────────────┘

2. GET /products      (Authorization: Bearer <token>)
        │
        ▼
   Gateway  ── JwtAuth filter ──▶ verify signature + expiry
        │                         extract claims
        │                         inject headers:
        │                           X-User-Id, X-User-Email, X-User-Roles
        ▼
   product-service  ──▶ reads headers, logs:
                          Current User : 1
                          Current Role : USER
                        returns the product data
```

If the token is missing, malformed, or invalid/expired, the gateway short-circuits with **401** and the request never reaches a backend service.

---

## What's inside the JWT

Signed with HS256 using a secret shared between user-service (signer) and gateway (verifier), passed via the `JWT_SECRET` environment variable.

| Claim | Meaning |
|-------|---------|
| `sub` | userId |
| `email` | user's email |
| `roles` | user's role(s), e.g. `USER` |
| `iat` | issued-at timestamp |
| `exp` | expiry (default 24h) |

---

## API

All requests go through the gateway at `http://localhost:8080`.

### Public (no token)
| Method | Path | Body | Purpose |
|--------|------|------|---------|
| POST | `/auth/register` | `{ email, password }` | create a user |
| POST | `/auth/login` | `{ email, password }` | get a JWT |

### Protected (require `Authorization: Bearer <token>`)
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/users/profile` | current user's profile |
| POST | `/products` | create a product |
| GET | `/products/{id}` | get one product |
| GET | `/products?page=0&size=10` | paginated list |

### Health (each service)
`GET /actuator/health` on `:8080`, `:8081`, `:8082`.

---

## Folder structure

```
microgateway/
├── docker-compose.yml          # postgres + 3 services
├── docker/init-databases.sql   # creates userdb + productdb
├── README.md
│
├── gateway/                    # reactive (WebFlux)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/yuvraj/gateway/
│       │   ├── GatewayApplication.java
│       │   ├── config/       JwtProperties, CorsConfig
│       │   └── filter/       JwtAuthGatewayFilterFactory, RequestLoggingFilter
│       └── resources/application.yml     # static routes live here
│
├── user-service/              # servlet (Web MVC + JPA), JWT issuer
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/yuvraj/userservice/
│       ├── controller/  AuthController, UserController
│       ├── service/     UserService, JwtService
│       ├── repository/  UserRepository
│       ├── entity/      UserEntity
│       ├── dto/         RegisterRequest, LoginRequest, AuthResponse, UserResponse
│       ├── config/      JwtProperties, BeanConfig, OpenApiConfig
│       └── exception/   GlobalExceptionHandler + custom exceptions
│
└── product-service/           # servlet (Web MVC + JPA), trusts gateway headers
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/yuvraj/productservice/
        ├── controller/  ProductController
        ├── service/     ProductService
        ├── repository/  ProductRepository
        ├── entity/      ProductEntity
        ├── dto/         CreateProductRequest, ProductResponse
        ├── security/    CurrentUser, AuthenticatedUser, CurrentUserArgumentResolver
        ├── config/      WebConfig, OpenApiConfig
        └── exception/   GlobalExceptionHandler + custom exceptions
```

---

## Running it

### With Docker (recommended)

```bash
# from the microgateway/ root
docker compose up --build
```

This starts Postgres (with `userdb` + `productdb`), both backend services, and the gateway. Only the gateway is exposed, on `http://localhost:8080`.

If port 8080 is already taken locally, override it:

```bash
GATEWAY_PORT=8090 docker compose up --build
```

Tear down (including the database volume):

```bash
docker compose down -v
```

### Try it end to end

```bash
# 1. Register
curl -s -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"password123"}'

# 2. Login → capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"password123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 3. Create a product (protected)
curl -s -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","description":"Mechanical","price":49.99}'

# 4. Paginated list
curl -s "http://localhost:8080/products?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# 5. Without a token → 401
curl -i -s http://localhost:8080/products
```

Watch the `product-service` logs during step 3/4 — you'll see `Current User` / `Current Role`, proving the gateway propagated identity via headers.

### Postman collection

A ready-to-run collection lives at [`postman/spring-api-gateway.postman_collection.json`](postman/spring-api-gateway.postman_collection.json) — the same end-to-end flow above (register → login → protected calls → bad-token rejection → health check), with automated assertions on each response. Import it into Postman and click **Run Collection**, or run it headless with [Newman](https://github.com/postmanlabs/newman):

```bash
npx newman run postman/spring-api-gateway.postman_collection.json
```

Login automatically captures the returned JWT into a collection variable, so every later request in the run reuses it — no manual copy-pasting of tokens between requests. The collection's `baseUrl` variable defaults to `http://localhost:8080`; if you overrode `GATEWAY_PORT` above, update `baseUrl` to match before running.

### Running a single service locally (without Docker)
Each service needs Java 21 and a reachable Postgres. Start Postgres via compose, then:
```bash
cd user-service && ./mvnw spring-boot:run
```

---

## Design notes

- **Reactive gateway, servlet backends.** The gateway uses WebFlux/Netty (never add `spring-boot-starter-web` to it); the backends use classic Web MVC + JPA. This mirrors how real Spring Cloud Gateway deployments are structured.
- **First-match routing.** Public routes (`/auth/register`, `/auth/login`) are declared **before** the protected catch-alls, because Spring Cloud Gateway matches routes top-down.
- **Stateless auth.** No server-side session; the JWT carries identity. The gateway rebuilds the same HS256 key from the shared secret to verify signatures.
- **Database per service.** `userdb` and `productdb` are separate, so neither service reaches into the other's data.
- **Centralized error handling.** Every service has a `@RestControllerAdvice` returning a consistent JSON error shape, including per-field validation messages.

---

## Future improvements (intentionally not built yet)

The code leaves clean seams for these — each is a natural next step:

- **Correlation ID** — add a trace-id header in `RequestLoggingFilter` and propagate it downstream (MDC).
- **Rate limiting** — a per-route rate-limit filter (Redis or Bucket4j) on the gateway.
- **Redis session validation** — a session-liveness check in `JwtAuthGatewayFilterFactory` (marked seam) for stateful revocation on top of stateless JWTs.
- **Resilience4j** — circuit breaker + retry + timeout on routed calls.
- **Monitoring** — Micrometer + Prometheus/Grafana dashboards.

---

## Notes

- The default `JWT_SECRET` and DB credentials in `docker-compose.yml` are development defaults. Override them via environment variables for any real deployment.
- Schema is auto-managed by Hibernate (`ddl-auto: update`) for convenience; a real system would use Flyway or Liquibase migrations.
