# Expense Tracker — Project Structure

## Overview

A **Spring Boot + Kotlin** backend for personal expense tracking with JWT authentication, role-based access control (RBAC), dynamic filtering, and financial reporting. The app is containerized with Docker and backed by PostgreSQL.

| Attribute | Value |
|---|---|
| Language | Kotlin 2.2.21 |
| Framework | Spring Boot 4.0.0 |
| Java | 21 |
| Database | PostgreSQL |
| Build Tool | Gradle (Kotlin DSL) |
| Auth | JWT (jjwt 0.12.3, HS256, 24h expiry) |

---

## Directory Tree

```
expensetrackerspring/
├── src/
│   ├── main/
│   │   ├── kotlin/com/devvikram/expensetracker/expensetracker/
│   │   │   ├── config/                    # App initialization & env config
│   │   │   ├── controllers/               # REST API layer
│   │   │   ├── dto/
│   │   │   │   ├── request/               # Inbound request payloads
│   │   │   │   └── response/              # Outbound response payloads
│   │   │   ├── entity/                    # JPA entities (DB models)
│   │   │   ├── enums/                     # Shared enumerations
│   │   │   ├── exceptions/                # Custom exceptions + global handler
│   │   │   ├── repository/                # Data access interfaces
│   │   │   ├── security/
│   │   │   │   └── anotation/             # Role-based security annotations
│   │   │   ├── service/                   # Business logic
│   │   │   ├── specifications/            # JPA Specifications for filtering
│   │   │   ├── flow/                      # Documentation files (this folder)
│   │   │   └── ExpensetrackerApplication.kt
│   │   └── resources/
│   │       ├── application.properties     # Base config
│   │       ├── application-dev.properties # Dev profile
│   │       └── application-prod.properties# Prod profile
│   └── test/
│       └── kotlin/...
│           └── ExpensetrackerApplicationTests.kt
├── build.gradle.kts                       # Gradle build + dependencies
├── settings.gradle.kts
├── Dockerfile                             # Multi-stage Docker build
└── .env                                   # Local env vars (not committed)
```

---

## Layer-by-Layer Breakdown

### `config/`

| File | Responsibility |
|---|---|
| `DataInitializer.kt` | Runs on startup: creates default roles and seeds the super-admin user if they don't exist. |
| `EnvConfig.kt` | Loads `.env` file via `dotenv-kotlin`; validates that all required variables (`JWT_SECRET`, `DATABASE_URL`, `PGUSER`, `PGPASSWORD`, `PORT`) are present before the context starts. |

---

### `controllers/`

REST controllers — thin layer that delegates all logic to services.

| File | Base Path | Key Endpoints |
|---|---|---|
| `AuthController.kt` | `/api/auth` | `POST /register`, `POST /login`, `GET /me`, `POST /assign-roles`, `GET /roles` |
| `ExpenseController.kt` | `/api/expenses` | Full CRUD + `GET /search`, `GET /filter/category`, `GET /filter/amount`, `GET /filter/date-range`, `POST /filter` (advanced) |
| `AdminCategoryController.kt` | `/api/admin/categories` | `POST`, `PUT /{id}`, `DELETE /{id}` — global categories (ADMIN role) |
| `UserCategoryController.kt` | `/api/categories` | `POST`, `GET`, `PUT /{id}`, `DELETE /{id}` — per-user categories |
| `ReportController.kt` | `/api/reports` | `GET /summary`, `GET /category-wise`, `GET /date-wise`, `POST /custom` (SUPER_ADMIN role) |

---

### `dto/`

Plain data classes with no business logic, used to decouple the API surface from internal entities.

#### Request DTOs (`dto/request/`)

| File | Fields |
|---|---|
| `LoginRequest.kt` | `email`, `password` |
| `RegisterRequest.kt` | `name`, `email`, `password`, `roles?` |
| `ExpenseRequest.kt` | `title`, `amount`, `categoryId`, `userId`, `note` |
| `ExpenseFilterRequest.kt` | `searchTitle?`, `categoryId?`, `minAmount?`, `maxAmount?`, `startDate?`, `endDate?`, `year?`, `month?` |
| `CustomReportRequest.kt` | `startDate`, `endDate`, `categoryIds`, `minAmount?`, `maxAmount?` |
| `AssignRoleRequest.kt` | `userId`, `roles: Set<RoleType>` |

#### Response DTOs (`dto/response/`)

| File | Fields |
|---|---|
| `ApiResponse<T>` | Generic wrapper — `status: Boolean`, `message: String`, `data: T` |
| `AuthResponse.kt` | `token`, `user: UserResponse` |
| `UserResponse.kt` | `id`, `name`, `email`, `roles`, `isActive`, `createdAt` |
| `ExpenseResponse.kt` | `id`, `title`, `amount`, `categoryId`, `categoryName`, `note`, `createdAt` |
| `SummaryReportResponse.kt` | `totalExpenses`, `count`, `averageAmount` |
| `CategoryWiseReportResponse.kt` | `categoryName`, `total`, `count`, `percentage` |
| `DateWiseReportResponse.kt` | `date`, `total`, `count` |

---

### `entity/`

JPA-annotated classes mapped to PostgreSQL tables.

| File | Table | Key Fields / Relationships |
|---|---|---|
| `User.kt` | `users` | `email (unique)`, `name`, `password (BCrypt)`, `isActive`, `createdAt`, `updatedAt` — M:M → `Role` |
| `Role.kt` | `roles` | `name: RoleType (enum)` — M:M → `User` |
| `Category.kt` | `categories` | `name`, `description`, `isGlobal: Boolean`, `userId (nullable)` — global categories have `userId = null` |
| `Expense.kt` | `expenses` | `title`, `amount (positive)`, `note`, `createdAt`, `userId` — M:1 → `Category` |

---

### `enums/`

| File | Values |
|---|---|
| `RoleType.kt` | `USER`, `ADMIN`, `SUPER_ADMIN`, `MODERATOR`, `ACCOUNTANT`, `VIEWER` |

---

### `exceptions/`

| File | Type / HTTP Status |
|---|---|
| `ResourceNotFoundException.kt` | Custom — mapped to **404** |
| `BadRequestException.kt` | Custom — mapped to **400** |
| `ConflictException.kt` | Custom — mapped to **409** (e.g. duplicate category) |
| `GlobalExceptionHandler.kt` | `@RestControllerAdvice` — catches all the above plus Spring/JWT exceptions and returns a consistent `ApiResponse` JSON error body |

**JWT exceptions handled:**
- `ExpiredJwtException` → 401
- `MalformedJwtException` → 400
- `SignatureException` → 401
- `AuthenticationException` → 401

---

### `repository/`

All interfaces extend `JpaRepository`; `ExpenseRepository` also extends `JpaSpecificationExecutor` for dynamic queries.

| Interface | Notable Custom Methods |
|---|---|
| `UserRepository` | `findByEmail()`, `existsByEmail()`, `findByEmailWithRoles()`, `findByRoleType()` |
| `ExpenseRepository` | `findByUserId()` — filtering uses Specifications, not named queries |
| `CategoryRepository` | Queries for global categories, user-specific categories, and combined lists |
| `RoleRepository` | `findByName()`, `existsByName()` |
| `ReportRepository` | Custom JPQL/native queries for `summaryReport()`, `categoryWiseReport()`, `getDateWiseReport()` |

---

### `security/`

| File | Responsibility |
|---|---|
| `SecurityConfig.kt` | Declares public endpoints (`/api/auth/register`, `/api/auth/login`, `/api/auth/roles`); all others require authentication; stateless session; plugs in `JwtAuthenticationFilter`. |
| `JwtUtil.kt` | Generates and validates JWT tokens; extracts claims (`userId`, `email`, `roles`); uses HS256; 24-hour expiry. |
| `JwtAuthenticationFilter.kt` | `OncePerRequestFilter` — extracts `Bearer` token from `Authorization` header, validates it, and sets `SecurityContextHolder`. |
| `JwtAuthenticationEntryPoint.kt` | Returns a JSON `401` response when authentication fails. |
| `SecurityUtil.kt` | Helper methods for getting the current authenticated user from the security context. |
| `CustomUserDetailsService.kt` | Implements `UserDetailsService`; loads user by email or ID; bridges JPA `User` entity with Spring Security. |

#### Security Annotations (`security/anotation/`)

Custom meta-annotations that combine `@PreAuthorize` with a role check, keeping controllers clean.

| Annotation | Required Role / Condition |
|---|---|
| `@IsAuthenticated` | Any authenticated user |
| `@IsAdmin` | `ADMIN` |
| `@IsSuperAdmin` | `SUPER_ADMIN` |
| `@IsModerator` | `MODERATOR` |
| `@IsAccountant` | `ACCOUNTANT` |
| `@IsOwner` | Resource ownership verified at runtime |

---

### `service/`

| File | Key Responsibilities |
|---|---|
| `AuthService.kt` | `register()` (hashes password, assigns default role), `login()` (validates credentials, issues JWT), `assignRoles()` (SUPER_ADMIN only) |
| `ExpenseService.kt` | CRUD for expenses; uses `ExpenseSpecifications` to compose dynamic filter queries; pagination via `Pageable` |
| `CategoryService.kt` | Manages both global (admin-controlled) and user-specific categories; prevents duplicate names within the same scope |
| `RoleService.kt` | Role lookup, creation, default role initialization |
| `ReportService.kt` | Aggregates data via `ReportRepository`: summary totals, category breakdown, date breakdown, custom-range reports |
| `CustomUserDetailsService.kt` | Spring Security integration — see Security section above |

---

### `specifications/`

| File | Purpose |
|---|---|
| `ExpenseSpecifications.kt` | Produces `Specification<Expense>` predicates for each filter dimension (user, title, category, amount range, date range, month/year). `buildFilterSpecification()` combines them with `and` for use in `ExpenseRepository.findAll(spec, pageable)`. |

---

## Configuration Files

### `application.properties` (base)

```properties
spring.application.name=expensetracker
server.port=${PORT:8081}
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${PGUSER}
spring.datasource.password=${PGPASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000          # 24 hours in ms
spring.jpa.database-platform=PostgreSQLDialect
# HikariCP pool: max=10, minIdle=5, connectionTimeout=20s
```

### `application-dev.properties`
- `ddl-auto=update` — Hibernate creates/alters tables automatically
- SQL logging at `DEBUG` level
- Full error details exposed in responses

### `application-prod.properties`
- `ddl-auto=validate` — schema must match entities; no auto-migration
- SQL logging at `WARN` level
- Error details hidden from API responses

---

## Dockerfile (Multi-stage)

```
Stage 1 — Builder (gradle:8.14-jdk21)
  └── gradle clean bootJar --no-daemon

Stage 2 — Runtime (eclipse-temurin:21-jre-alpine)
  ├── Non-root user: spring:spring
  ├── Exposes port 8080
  └── JVM flags: -Xmx512m -Xms256m
```

All secrets are passed as environment variables at runtime; nothing is baked into the image.

---

## Required Environment Variables

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Signing key for JWT tokens |
| `DATABASE_URL` | Full JDBC URL for PostgreSQL |
| `PGUSER` | Database username |
| `PGPASSWORD` | Database password |
| `PORT` | HTTP port (default `8081`) |

---

## Architecture Flow

```
HTTP Request
     │
     ▼
JwtAuthenticationFilter          ← validates Bearer token, sets SecurityContext
     │
     ▼
SecurityConfig (route guards)    ← enforces role annotations
     │
     ▼
Controller                       ← parses request DTO, calls service
     │
     ▼
Service                          ← business logic, validation, orchestration
     │
     ▼
Repository / Specifications      ← database queries (JPA + dynamic specs)
     │
     ▼
PostgreSQL
     │
     ▼
Response DTO → ApiResponse<T>    ← wrapped in generic envelope, returned as JSON
```

---

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Stateless JWT | No server-side session storage; horizontally scalable |
| `ApiResponse<T>` wrapper | Consistent envelope for all responses — success and error |
| JPA Specifications | Avoids N query methods; builds complex filters dynamically at runtime |
| Global vs. User categories | System-wide defaults managed by admins; users can extend with private categories |
| Custom security annotations | Reduces boilerplate; centralises role logic away from controller methods |
| `ddl-auto=validate` in prod | Prevents accidental schema changes; migrations handled manually or via Flyway |
| Multi-stage Docker build | Keeps the final image small (JRE only); build tools not shipped to production |
