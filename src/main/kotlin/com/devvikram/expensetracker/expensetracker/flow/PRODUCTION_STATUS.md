# Production Status — Expense Tracker Spring

> **Kotlin 2.2 • Spring Boot 4.0 • PostgreSQL • JWT • Docker**
> Last updated: 2026-04-04 | Branch: `dev-feature/notification-dashboard`

---

## Last Work Done

### Committed (HEAD) — `feat: add receipt attachments (DB storage) + fix category module bugs`
- Receipt upload/download stored as BYTEA in PostgreSQL (`V14`, `V15` migrations)
- `V16` migration: extended `audit_logs_action_check` constraint with `RECEIPT_UPLOADED`, `RECEIPT_DELETED`
- `GET /{receiptId}/download` endpoint streams raw file bytes with correct `Content-Type`
- `UserCategoryController`: fixed `@IsSuperAdmin` → `@IsAuthenticated` (regular users were locked out)
- `CategoryService`: wired `AuditLogService`; logs `CATEGORY_CREATED / UPDATED / DELETED`
- Category delete guard: blocks deletion if linked expenses, budgets, or recurring expenses exist
- `AdminCategoryController`: added `GET /api/v1/admin/categories` list endpoint
- `CategoryRequest` DTO introduced to replace raw entity in request bodies

### In-Progress (staged, not committed)
- **Refresh Token system**
  - `RefreshToken` entity + `RefreshTokenRepository`
  - `RefreshTokenService` — single active session, revoke-on-login, 7-day expiry
  - `V17__create_refresh_tokens.sql` migration
  - `POST /api/v1/auth/refresh` — exchange refresh token for new token pair
  - `POST /api/v1/auth/logout` — revoke current user's refresh token
  - `JwtAuthenticationFilter` updated to handle token validation edge cases
- Docs reorganized: old scattered files deleted, consolidated into `flow/guide/` and `flow/postman_collections/`

---

## Feature Completeness

| Module | Status | Notes |
|---|---|---|
| Auth (register / login) | ✅ Done | BCrypt, JWT HMAC-SHA256 |
| JWT Access Token (15 min) | ✅ Done | Stateless, filter-based |
| Refresh Token (7 days) | 🔄 In-Progress | Staged, not committed (V17) |
| Logout / Token Revocation | 🔄 In-Progress | Staged, not committed |
| Role-Based Access Control | ✅ Done | 6 roles, custom annotations |
| Categories (user + admin) | ✅ Done | Access bug fixed, delete guard added |
| Expense CRUD | ✅ Done | Ownership checks, soft delete |
| Dynamic Filtering (JPA Spec) | ✅ Done | Composable filters |
| Budgets | ✅ Done | Period reset, alert threshold, spent tracking |
| Recurring Expenses | ✅ Done | @Scheduled daily job, auto-creates expense |
| Audit Logging | ✅ Done | Wired into all services, 5 controller endpoints |
| Notifications | ✅ Done | 4 types, auto-triggered by budget & scheduler |
| Dashboard API | ✅ Done | Monthly summary, budgets, recent, upcoming |
| Tags (Many-to-Many) | ✅ Done | Per-user, filterable, V9 migration |
| Multi-Currency | ✅ Done | ExchangeRateService, USD pivot, daily sync |
| Receipt Attachments | ✅ Done | BYTEA in DB, 5 MB limit, jpg/png/pdf |
| Reports (Summary/Category/Date) | ✅ Done | |
| Trend Reports | ✅ Done | Monthly trend + budget performance |
| Export (CSV / PDF) | ✅ Done | Apache Commons CSV + OpenPDF |
| Swagger / OpenAPI | ✅ Done | `/swagger-ui.html`, JWT bearer auth wired |
| Flyway Migrations | ✅ Done | V1–V17 |
| Docker | ✅ Done | Multi-stage build, non-root user, env vars |
| Tests | ✅ Done | 45 tests: unit + integration |
| Actuator Health | ✅ Done | `/actuator/health`, `/actuator/info` |
| Rate Limiting | ❌ Missing | No brute-force protection on login |
| CORS Configuration | ❌ Missing | No `cors()` in SecurityConfig |
| Email Verification | ❌ Missing | Any email registers without verification |
| Password Reset Flow | ❌ Missing | No forgot/reset password endpoints |
| Redis Caching | ❌ Missing | Reports hit DB every time |
| Live Exchange Rate Sync | ❌ Missing | No scheduled job for open.er-api.com |

---

## Production Readiness — Pending Items

### 🔴 Critical (blockers before going live)

#### 1. Commit the Refresh Token Feature
The `RefreshToken` entity, service, repository, V17 migration, and auth endpoints are all staged but not committed.
V17 migration won't run on any deployment until this is committed.

#### 2. CORS Configuration Missing
`SecurityConfig.kt` has no `cors()` block. Any browser-based frontend will get blocked by CORS policy.

```kotlin
// Add inside securityFilterChain:
http.cors { it.configurationSource(corsConfigurationSource()) }

@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val config = CorsConfiguration()
    config.allowedOrigins = listOf("https://your-frontend.com")
    config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    config.allowedHeaders = listOf("*")
    config.allowCredentials = true
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", config)
    return source
}
```

#### 3. Swagger Open in Production
`/swagger-ui/**` and `/v3/api-docs/**` are publicly accessible. Should be restricted in prod.

```kotlin
// In SecurityConfig, restrict Swagger behind auth in prod profile:
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("SUPER_ADMIN")
```

#### 4. Hardcoded Fallback Credentials
`application.properties` contains:
```
spring.security.user.name=admin
spring.security.user.password=admin123
```
These are Spring Boot's fallback basic-auth credentials. Remove or override with a secure env variable.

---

### 🟡 Important (should fix soon)

#### 5. No Rate Limiting on Auth Endpoints
`/api/v1/auth/login` has no brute-force protection. Add Bucket4j or Spring Security's built-in rate limiter.
- Max 5 attempts per IP per minute on `/login` and `/register`

#### 6. No Email Verification
Users register with any email — no OTP or link sent. Add a `verified` flag on `User` and block login until verified.

#### 7. No Password Reset Flow
Missing: `POST /auth/forgot-password` and `POST /auth/reset-password` endpoints.

#### 8. Exchange Rate Sync Not Scheduled
`ExchangeRateService` references `open.er-api.com` but there is no `@Scheduled` job to refresh rates daily.
Rates in DB will become stale without manual seeding.

#### 9. Receipt Storage Won't Scale
Storing file bytes as `BYTEA` in PostgreSQL works for low traffic but causes DB bloat at scale.
Consider migrating to S3/MinIO when volume increases.

#### 10. `SPRING_PROFILES_ACTIVE` Not Enforced
`application-prod.properties` exists with `ddl-auto=validate` and suppressed error details, but nothing in Dockerfile or docker-compose forces `SPRING_PROFILES_ACTIVE=prod` at runtime.

---

### 🟢 Nice to Have

#### 11. Test Coverage Gaps
Tests exist for Auth, Budget, Expense, Notification, and Security integration.
Missing tests for: Receipts, Tags, Refresh Tokens, Exchange Rates, Recurring Expenses.

#### 12. Centralized Logging
No Logback appender config for external log aggregation (ELK, Loki, Datadog).
Currently only console logging.

#### 13. API Pagination
Most list endpoints return all records. Consider adding `Pageable` to `GET /api/v1/expenses`, `/audit-logs`, `/notifications`.

---

## DB Migration Timeline

| Version | Description |
|---|---|
| V1 | roles + users |
| V2 | categories |
| V3 | expenses |
| V4 | budgets |
| V5 | recurring_expenses |
| V6 | audit_logs |
| V7 | ensure audit_logs (repair) |
| V8 | notifications |
| V9 | tags (many-to-many) |
| V10 | currency column on expenses |
| V11 | base_currency on users |
| V12 | exchange_rates table |
| V13 | fix missing columns |
| V14 | receipts table (BYTEA) |
| V15 | migrate existing receipts (drop s3_key, add file_data) |
| V16 | audit_logs: add RECEIPT_UPLOADED / RECEIPT_DELETED actions |
| V17 | refresh_tokens table (staged, not committed) |

---

## Environment Variables Required

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `PGUSER` | DB username |
| `PGPASSWORD` | DB password |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) |
| `PORT` | Server port (default: 8081) |
| `SPRING_PROFILES_ACTIVE` | Set to `prod` in production |
