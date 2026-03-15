# Expense Tracker — Flow Diagrams

> All diagrams use [Mermaid](https://mermaid.js.org/) syntax — rendered natively in GitHub, GitLab, JetBrains IDEs, and VS Code (with the Mermaid Preview extension).

---

## 1. High-Level Architecture

```mermaid
graph TB
    Client["🌐 Client\n(Postman / Frontend)"]

    subgraph Docker["Docker Container"]
        subgraph App["Spring Boot 4.0 Application"]
            Filter["JwtAuthenticationFilter\nOncePerRequestFilter"]
            Security["SecurityConfig\nRoute Guards + Role Enforcement"]
            Controllers["Controllers Layer\n12 REST Controllers\n/api/v1/*"]
            Services["Service Layer\n15 Services"]
            Specs["JPA Specifications\nDynamic Filters"]
            Repos["Repository Layer\n12 Repositories"]
        end
    end

    subgraph DB["PostgreSQL"]
        Tables["12 Tables\nusers · roles · expenses · categories\nbudgets · recurring_expenses · tags\nnotifications · audit_logs · exchange_rates\nexpense_tags · user_roles"]
    end

    subgraph External["External Services"]
        ExRateAPI["open.er-api.com\nFree Exchange Rate API\n~160 currencies"]
        Swagger["Swagger UI\n/swagger-ui.html"]
    end

    Client -->|"Bearer JWT"| Filter
    Filter --> Security
    Security --> Controllers
    Controllers --> Services
    Services --> Specs
    Specs --> Repos
    Repos --> Tables
    Services -->|"daily @Scheduled\n01:00 AM"| ExRateAPI
    Client -->|"No auth required"| Swagger
```

---

## 2. Request Lifecycle (Every Authenticated Request)

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthFilter
    participant S as SecurityConfig
    participant Ctrl as Controller
    participant Svc as Service
    participant AL as AuditLogService
    participant DB as PostgreSQL

    C->>F: HTTP Request + "Authorization: Bearer <token>"
    F->>F: Extract token from header
    F->>DB: Load user by email (CustomUserDetailsService)
    F->>F: Validate token (signature, expiry, claims)

    alt Token Invalid / Expired
        F-->>C: 401 Unauthorized (JwtAuthenticationEntryPoint)
    end

    F->>S: Set SecurityContext, continue filter chain
    S->>S: Check @PreAuthorize / role annotation
    alt Insufficient Role
        S-->>C: 403 Forbidden
    end

    S->>Ctrl: Request reaches controller
    Ctrl->>Ctrl: Deserialize + @Valid request DTO
    alt Validation Failed
        Ctrl-->>C: 400 ErrorResponse { code: VALIDATION_FAILED, details: {field: msg} }
    end

    Ctrl->>Svc: Call service method
    Svc->>DB: Query / Persist via Repository
    Svc->>AL: log() in REQUIRES_NEW transaction
    AL->>DB: INSERT audit_log (async, never throws)
    Svc-->>Ctrl: Return result
    Ctrl-->>C: 200 ApiResponse { status, message, data }
```

---

## 3. Authentication Flow

```mermaid
flowchart TD
    A([Start]) --> B{Endpoint?}

    B -->|POST /register| R1[Validate RegisterRequest]
    R1 --> R2{Email exists?}
    R2 -->|Yes| R3[throw IllegalArgumentException\nGlobalExceptionHandler → 401\ncode: INVALID_CREDENTIALS]
    R2 -->|No| R4[BCrypt hash password]
    R4 --> R5[Create User entity\nAssign USER role\n+ any extra roles from request]
    R5 --> R6[userRepository.save]
    R6 --> R7[AuditLog: USER_REGISTERED]
    R7 --> R8[jwtUtil.generateToken]
    R8 --> R9[Return AuthResponse\ntoken + UserResponse + baseCurrency]

    B -->|POST /login| L1[Validate LoginRequest]
    L1 --> L2[userRepository.findByEmailWithRoles]
    L2 --> L3{User found?}
    L3 -->|No| L4[throw IllegalArgumentException → 401]
    L3 -->|Yes| L5{isActive?}
    L5 -->|No| L6[throw IllegalArgumentException → 401\nAccount deactivated]
    L5 -->|Yes| L7{Password matches?}
    L7 -->|No| L8[throw IllegalArgumentException → 401]
    L7 -->|Yes| L9[AuditLog: USER_LOGIN]
    L9 --> L10[jwtUtil.generateToken]
    L10 --> L11[Return AuthResponse]

    B -->|PUT /me/currency| C1[Validate UpdateBaseCurrencyRequest\n3 uppercase letters]
    C1 --> C2[userRepository.findById]
    C2 --> C3[user.copy baseCurrency = newCurrency]
    C3 --> C4[userRepository.save]
    C4 --> C5[AuditLog: USER_CURRENCY_UPDATED]
    C5 --> C6[Return updated UserResponse]
```

---

## 4. Expense Creation Flow (Full Pipeline)

```mermaid
flowchart TD
    Start([POST /api/v1/expenses]) --> V[Validate ExpenseRequest\ntitle · amount · categoryId · userId · currency · tagIds]
    V --> BC[BudgetService.checkBudgetOnExpense\nuserId · categoryId · amount]

    BC --> B1[Find all active budgets for user\ncategory-scoped + overall]
    B1 --> B2{Any budgets?}
    B2 -->|No| CAT
    B2 -->|Yes| B3[For each budget: compute spent in period]
    B3 --> B4{spent + amount >= budget.amount?}
    B4 -->|Yes — shouldBlock=true| B5[Add BUDGET_EXCEEDED warning\nSend BUDGET_EXCEEDED notification]
    B4 -->|No| B6{spent + amount >= alertThreshold?}
    B6 -->|Yes| B7[Add BUDGET_ALERT warning\nSend BUDGET_ALERT notification]
    B6 -->|No| CAT
    B5 --> BLOCK{shouldBlock?}
    B7 --> CAT
    BLOCK -->|Yes| ERR[throw BadRequestException\nGlobalExceptionHandler → 400\ncode: BAD_REQUEST]
    BLOCK -->|No| CAT

    CAT[categoryRepository.findById categoryId] --> C1{Found?}
    C1 -->|No| E1[throw ResourceNotFoundException → 404\ncode: RESOURCE_NOT_FOUND]
    C1 -->|Yes| TAGS

    TAGS[Resolve tags by ownership\ntagRepository.findById for each tagId\nfilter by userId] --> CUR

    CUR[ExchangeRateService.convert\namount · expense.currency → user.baseCurrency] --> CUR1[Lookup rateUSD→expenseCurrency\nLookup rateUSD→baseCurrency]
    CUR1 --> CUR2[amountInBase = amount × rateB / rateA\nFallback: 1:1 if rates not loaded]
    CUR2 --> SAVE

    SAVE[expenseRepository.save\nExpense: title · amount · currency · amountInBase\ncategory · userId · note · tags] --> AUD
    AUD[AuditLogService.log\nEXPENSE_CREATED\nentityId=expense.id\nnewValue=ExpenseResponse JSON] --> RET

    RET([Return ExpenseResponse\nid · title · amount · currency · amountInBase\ncategoryId · categoryName · note · createdAt · tags])
```

---

## 5. Multi-Currency Flow

```mermaid
flowchart LR
    subgraph Sync["Daily Sync — 01:00 AM"]
        SC1[ExchangeRateService.scheduledSync] --> SC2["GET open.er-api.com/v6/latest/USD"]
        SC2 --> SC3{result == success?}
        SC3 -->|No| SC4[Log error, return 0]
        SC3 -->|Yes| SC5["For each currency in rates map\n~160 currencies"]
        SC5 --> SC6{Rate exists in DB?}
        SC6 -->|Yes| SC7[UPDATE rate + fetchedAt]
        SC6 -->|No| SC8[INSERT new ExchangeRate]
    end

    subgraph Convert["Runtime Conversion"]
        CV1["convert(amount, from, to)"] --> CV2{from == to?}
        CV2 -->|Yes| CV3[return amount unchanged]
        CV2 -->|No| CV4[Lookup rateUSD→from\nLookup rateUSD→to]
        CV4 --> CV5{Both rates found?}
        CV5 -->|No| CV6[Log warning\nreturn amount as-is fallback]
        CV5 -->|Yes| CV7["amountInBase = amount × (rateTo / rateFrom)"]
    end

    subgraph Example["Example: USD 100 → INR"]
        E1["rateUSD→USD = 1.0\nrateUSD→INR = 83.5"]
        E2["amountInBase = 100 × (83.5 / 1.0) = ₹8350"]
    end

    subgraph UserPref["User Base Currency"]
        UP1["PUT /api/v1/auth/me/currency\n{ baseCurrency: 'USD' }"]
        UP1 --> UP2[user.copy baseCurrency = 'USD']
        UP2 --> UP3[All future expenses converted to USD]
    end
```

---

## 6. Scheduled Jobs Timeline

```mermaid
gantt
    title Daily Scheduled Jobs
    dateFormat HH:mm
    axisFormat %H:%M

    section Budget Reset
    BudgetService.resetPeriodicBudgets()     : 00:00, 5m

    section Recurring Expenses
    RecurringExpenseService.processRecurring() : 00:05, 15m

    section Exchange Rates
    ExchangeRateService.scheduledSync()       : 01:00, 10m
```

```mermaid
flowchart TD
    subgraph Job1["00:00 — Budget Period Reset"]
        J1A[Find all active budgets\nwhere period window has elapsed] --> J1B[For each: advance startDate\nby one period length]
        J1B --> J1C[budgetRepository.save]
    end

    subgraph Job2["00:05 — Recurring Expense Processor"]
        J2A["findAllDueToday\nnextDueDate <= today\nAND isActive = true\nAND endDate >= today OR null"] --> J2B[For each entry independently]
        J2B --> J2C[Build ExpenseRequest\ntitle · amount · categoryId · userId]
        J2C --> J2D[expenseService.createExpense\nbudget check + currency convert + audit]
        J2D --> J2E[AuditLog: EXPENSE_AUTO_CREATED]
        J2E --> J2F[Send RECURRING_EXPENSE_DUE notification]
        J2F --> J2G{endDate passed?}
        J2G -->|Yes| J2H[set isActive = false]
        J2G -->|No| J2I[Advance nextDueDate by frequency]
        J2I --> J2J[recurringExpenseRepository.save]
        J2B -->|Exception for one entry| J2K[Log error, continue to next entry]
    end

    subgraph Job3["01:00 — Exchange Rate Sync"]
        J3A["GET open.er-api.com/v6/latest/USD"] --> J3B[Parse rates map ~160 currencies]
        J3B --> J3C[Upsert each rate in exchange_rates table]
        J3C --> J3D[Log count updated]
        J3A -->|Network/API error| J3E[Log error, swallow exception\nApp continues normally]
    end
```

---

## 7. Budget Enforcement Flow

```mermaid
flowchart TD
    CE[ExpenseService.createExpense called] --> BC[BudgetService.checkBudgetOnExpense\nuserId · categoryId · newAmount]

    BC --> Q1[Query 1: budgets WHERE userId=X\nAND categoryId=Y AND isActive=true\nAND deletedAt IS NULL AND period active]
    BC --> Q2[Query 2: budgets WHERE userId=X\nAND categoryId IS NULL\noverall budget]

    Q1 --> LOOP[Process each matching budget]
    Q2 --> LOOP

    LOOP --> SPENT[expenseRepository.sumAmount\nfor userId + categoryId + period window]
    SPENT --> CALC[projected = spent + newAmount]

    CALC --> CHECK1{projected >= budget.amount?}
    CHECK1 -->|Yes| N1[NotificationService.send\ntype: BUDGET_EXCEEDED\ntitle: 'Budget Exceeded']
    N1 --> BLOCK[BudgetCheckResult.shouldBlock = true\nadd warning message]

    CHECK1 -->|No| CHECK2{projected >= budget.amount × alertThreshold?}
    CHECK2 -->|Yes| N2[NotificationService.send\ntype: BUDGET_ALERT\ntitle: 'Budget Alert']
    N2 --> WARN[Add warning to list\nshouldBlock remains false]
    CHECK2 -->|No| OK[No action for this budget]

    BLOCK --> RESULT[Return BudgetCheckResult]
    WARN --> RESULT
    OK --> RESULT

    RESULT --> ES{shouldBlock?}
    ES -->|Yes| ERR[throw BadRequestException\nExpense NOT saved]
    ES -->|No| PROCEED[Continue expense creation]
```

---

## 8. Notification Flow

```mermaid
flowchart LR
    subgraph Triggers["Notification Triggers"]
        T1["BudgetService.checkBudgetOnExpense\n→ BUDGET_EXCEEDED or BUDGET_ALERT"]
        T2["RecurringExpenseService\n.processSingleRecurringExpense\n→ RECURRING_EXPENSE_DUE"]
        T3["Any service can call\nnotificationService.send()"]
    end

    subgraph Service["NotificationService"]
        S1["send(userId, title, message, type,\nentityType?, entityId?)"]
        S1 --> S2[notificationRepository.save\nNotification entity]
        S2 --> S3{Exception?}
        S3 -->|Yes| S4[Log error, swallow\nNever interrupts caller]
        S3 -->|No| S5[Notification stored]
    end

    subgraph Consumer["Notification Endpoints"]
        E1["GET /api/v1/notifications\nAll, paginated, newest first"]
        E2["GET /api/v1/notifications/unread\nUnread only"]
        E3["GET /api/v1/notifications/unread/count\nBadge number"]
        E4["PUT /api/v1/notifications/{id}/read\nMark one read → NOTIFICATION_READ audit"]
        E5["PUT /api/v1/notifications/read-all\nMark all read"]
        E6["DELETE /api/v1/notifications/{id}\n→ NOTIFICATION_DELETED audit"]
    end

    T1 --> S1
    T2 --> S1
    T3 --> S1
    S5 --> Consumer
```

---

## 9. Tags / Labels Flow

```mermaid
flowchart TD
    subgraph CRUD["Tag CRUD — /api/v1/tags"]
        CT[POST /tags\nTagRequest: name · color] --> CT1{name exists\nfor this user?}
        CT1 -->|Yes| CT2[throw ConflictException → 409\ncode: CONFLICT]
        CT1 -->|No| CT3[tagRepository.save\nTag: name · color · userId · createdAt]
        CT3 --> CT4[AuditLog: TAG_CREATED]
        CT4 --> CT5[Return TagResponse]

        UT[PUT /tags/{id}\nTagRequest: name · color] --> UT1[findByIdAndUserId\nOwnership enforced]
        UT1 -->|Not found| UT2[throw ResourceNotFoundException → 404]
        UT1 -->|Found| UT3{Name changed\nAND new name exists?}
        UT3 -->|Yes| UT4[throw ConflictException → 409]
        UT3 -->|No| UT5[tag.copy name · color]
        UT5 --> UT6[tagRepository.save]
        UT6 --> UT7[AuditLog: TAG_UPDATED]

        DT[DELETE /tags/{id}] --> DT1[findByIdAndUserId]
        DT1 --> DT2[tagRepository.deleteById\nCASCADES to expense_tags]
        DT2 --> DT3[AuditLog: TAG_DELETED]
    end

    subgraph Attach["Tags on Expenses"]
        EA[ExpenseRequest includes\ntagIds: List&lt;Long&gt;] --> EA1[resolveTagsForUser\nfor each tagId: findById\nfilter: tag.userId == caller userId]
        EA1 --> EA2[expense.tags = resolvedTags\nSaved via ManyToMany EAGER\nexpense_tags join table]
    end

    subgraph Filter["Filter Expenses by Tags"]
        FF[POST /api/v1/expenses/filter\nExpenseFilterRequest.tagIds = 1,3] --> FF1[filterByTagIds Specification\nINNER JOIN expense_tags ON tag.id IN tagIds\nquery.distinct = true]
        FF1 --> FF2[Returns only expenses\nhaving ALL specified tags]
    end
```

---

## 10. Report Generation Flow

```mermaid
flowchart TD
    subgraph DB_Reports["DB-Backed Reports (ReportRepository)"]
        R1["GET /summary\nJPQL: SUM amount · COUNT · AVG"] --> RR[ReportRepository\nnative/JPQL queries]
        R2["GET /category-wise\nGROUP BY category"] --> RR
        R3["GET /date-wise\nGROUP BY DATE(created_at)"] --> RR
        R4["POST /custom\ndate range + category filter\nvia Specifications"] --> RR
        R5["GET /trends?months=6\nEXTRACT YEAR/MONTH\nwith category breakdown"] --> RR
        R6["GET /budget-performance\nLive spent vs limit per budget"] --> BS[BudgetService\n.getBudgetStatus per budget]
    end

    subgraph Memory_Reports["In-Memory Reports (ReportService)"]
        R7["GET /insights"] --> INS[Load all user expenses\nfrom expenseRepository]
        INS --> INS1[This month vs last month totals]
        INS --> INS2[Velocity: INCREASING / DECREASING / STABLE]
        INS --> INS3[Avg daily spend — last 30 days]
        INS --> INS4[Highest spend day — last 30 days]
        INS --> INS5[Biggest single expense — all time]
        INS --> INS6[Most used category — by count]

        R8["GET /top-expenses?limit=10&categoryId="] --> TOP[Load expenses by userId\noptional category Specification]
        TOP --> TOP1[Sort by amount DESC]
        TOP1 --> TOP2[Take first N — capped 1-100]
    end

    subgraph Export["File Export"]
        R9["GET /export?format=csv"] --> EXP[ExportService\n@Transactional readOnly=true]
        R10["GET /export?format=pdf"] --> EXP
        EXP --> EXP1[findByUserId sorted by createdAt DESC]
        EXP1 -->|csv| EXP2[Apache Commons CSV\nContent-Type: text/csv\nfilename: expenses.csv]
        EXP1 -->|pdf| EXP3[OpenPDF table\nIndigo header · alternating rows\nfilename: expenses.pdf]
        EXP2 --> EXPAUD[AuditLog: REPORT_EXPORTED]
        EXP3 --> EXPAUD
    end
```

---

## 11. Audit Logging Flow

```mermaid
flowchart LR
    subgraph Writers["Who Writes Audit Logs"]
        W1[ExpenseService\nCREATED · UPDATED · DELETED · AUTO_CREATED]
        W2[BudgetService\nCREATED · UPDATED · DELETED]
        W3[RecurringExpenseService\nCREATED · UPDATED · DELETED · PROCESSED]
        W4[CategoryService\nCREATED · UPDATED · DELETED]
        W5[AuthService\nUSER_REGISTERED · USER_LOGIN · ROLE_ASSIGNED\nUSER_CURRENCY_UPDATED]
        W6[NotificationService\nNOTIFICATION_READ · NOTIFICATION_DELETED]
        W7[ExportService\nREPORT_EXPORTED]
        W8[TagService\nTAG_CREATED · UPDATED · DELETED]
    end

    subgraph AuditLogService["AuditLogService.log()"]
        AL1["@Transactional(REQUIRES_NEW)\nOwn transaction — never blocked by caller rollback"]
        AL2[Capture IP: X-Forwarded-For → remoteAddr → null for scheduler]
        AL3["Serialize oldValue / newValue to JSON\nvia ObjectMapper (Jackson 3.x — tools.jackson)"]
        AL4[auditLogRepository.save]
        AL5{Exception?}
        AL5 -->|Yes| AL6[Log error, swallow\nCaller is NEVER affected]
        AL5 -->|No| AL7[AuditLog persisted]
    end

    subgraph Endpoints["Read Endpoints — /api/v1/audit-logs"]
        E1[GET / — All logs paginated — ADMIN]
        E2[GET /me — My audit trail — Authenticated]
        E3[GET /user/{userId} — Any user — SUPER_ADMIN]
        E4[GET /entity/{type}/{id} — Entity history — ADMIN]
        E5[GET /action/{action} — Filter by action — ADMIN]
    end

    W1 & W2 & W3 & W4 & W5 & W6 & W7 & W8 --> AL1
    AL1 --> AL2 --> AL3 --> AL4 --> AL5
    AL7 --> Endpoints
```

---

## 12. Entity Relationship Diagram

```mermaid
erDiagram
    USER {
        bigint id PK
        string email UK
        string name
        string password
        boolean isActive
        string baseCurrency
        timestamp createdAt
        timestamp updatedAt
    }

    ROLE {
        bigint id PK
        string name
    }

    USER_ROLES {
        bigint user_id FK
        bigint role_id FK
    }

    CATEGORY {
        bigint id PK
        string name
        string description
        boolean isGlobal
        bigint user_id FK_nullable
    }

    EXPENSE {
        bigint id PK
        string title
        double amount
        string currency
        double amount_in_base
        bigint category_id FK
        bigint user_id FK
        string note
        timestamp createdAt
    }

    TAG {
        bigint id PK
        string name
        string color
        bigint user_id FK
        timestamp createdAt
    }

    EXPENSE_TAGS {
        bigint expense_id FK
        bigint tag_id FK
    }

    BUDGET {
        bigint id PK
        bigint user_id FK
        bigint category_id FK_nullable
        double amount
        string period
        date startDate
        date endDate
        double alertThreshold
        boolean isActive
        timestamp deletedAt
        timestamp createdAt
    }

    RECURRING_EXPENSE {
        bigint id PK
        bigint user_id FK
        bigint category_id FK
        string title
        double amount
        string frequency
        date nextDueDate
        date endDate
        boolean isActive
        string note
        timestamp deletedAt
        timestamp createdAt
    }

    NOTIFICATION {
        bigint id PK
        bigint user_id FK
        string title
        string message
        string type
        boolean isRead
        string entityType
        bigint entityId
        timestamp createdAt
    }

    AUDIT_LOG {
        bigint id PK
        bigint user_id FK
        string action
        string entityType
        bigint entityId
        text oldValue
        text newValue
        string ipAddress
        timestamp createdAt
    }

    EXCHANGE_RATE {
        bigint id PK
        string base_currency
        string target_currency
        double rate
        timestamp fetchedAt
    }

    USER ||--o{ USER_ROLES : "has"
    ROLE ||--o{ USER_ROLES : "assigned via"
    USER ||--o{ EXPENSE : "owns"
    USER ||--o{ BUDGET : "has"
    USER ||--o{ RECURRING_EXPENSE : "has"
    USER ||--o{ NOTIFICATION : "receives"
    USER ||--o{ AUDIT_LOG : "generates"
    USER ||--o{ TAG : "owns"
    CATEGORY ||--o{ EXPENSE : "categorizes"
    CATEGORY ||--o{ BUDGET : "scopes"
    CATEGORY ||--o{ RECURRING_EXPENSE : "categorizes"
    EXPENSE ||--o{ EXPENSE_TAGS : "tagged via"
    TAG ||--o{ EXPENSE_TAGS : "applied via"
```

---

## 13. Error Response Flow

```mermaid
flowchart TD
    EX[Exception thrown in any Service / Controller]

    EX --> GEH["@RestControllerAdvice\nGlobalExceptionHandler"]

    GEH --> WHICH{Exception type?}

    WHICH -->|ResourceNotFoundException| E404["404 NOT FOUND\ncode: RESOURCE_NOT_FOUND"]
    WHICH -->|UsernameNotFoundException| E404U["404 NOT FOUND\ncode: USER_NOT_FOUND"]
    WHICH -->|MethodArgumentNotValidException| E400V["400 BAD REQUEST\ncode: VALIDATION_FAILED\ndetails: { field: message }"]
    WHICH -->|BadRequestException| E400["400 BAD REQUEST\ncode: BAD_REQUEST"]
    WHICH -->|ConflictException| E409["409 CONFLICT\ncode: CONFLICT"]
    WHICH -->|AuthenticationException| E401A["401 UNAUTHORIZED\ncode: AUTHENTICATION_FAILED"]
    WHICH -->|IllegalArgumentException| E401I["401 UNAUTHORIZED\ncode: INVALID_CREDENTIALS"]
    WHICH -->|ExpiredJwtException| E401JWT["401 UNAUTHORIZED\ncode: TOKEN_EXPIRED"]
    WHICH -->|MalformedJwtException| E400JWT["400 BAD REQUEST\ncode: TOKEN_INVALID"]
    WHICH -->|SignatureException| E401SIG["401 UNAUTHORIZED\ncode: TOKEN_SIGNATURE_INVALID"]
    WHICH -->|Exception catch-all| E500["500 INTERNAL SERVER ERROR\ncode: INTERNAL_ERROR\nLogged via SLF4J"]

    E404 & E404U & E400V & E400 & E409 & E401A & E401I & E401JWT & E400JWT & E401SIG & E500 --> RESP

    RESP["ErrorResponse JSON\n{\n  status: false,\n  code: 'RESOURCE_NOT_FOUND',\n  message: 'Expense not found',\n  timestamp: '2026-03-15T10:30:00',\n  path: '/api/v1/expenses/42',\n  details: null\n}"]
```

---

## 14. Security Layer Decision Tree

```mermaid
flowchart TD
    REQ[Incoming HTTP Request] --> PUB{Is path public?}

    PUB -->|Yes — /api/v1/auth/register\n/api/v1/auth/login\n/api/v1/auth/roles\n/swagger-ui/**\n/v3/api-docs/**\n/actuator/health| PASS[Pass through — no auth needed]

    PUB -->|No| TOKEN{Authorization\nheader present?}
    TOKEN -->|Missing| R401A[401 Unauthorized\nJwtAuthenticationEntryPoint]

    TOKEN -->|Present| VALID{JWT valid?\nsignature + expiry}
    VALID -->|Expired| R401B[401 — TOKEN_EXPIRED]
    VALID -->|Malformed| R400[400 — TOKEN_INVALID]
    VALID -->|Bad signature| R401C[401 — TOKEN_SIGNATURE_INVALID]
    VALID -->|Valid| LOAD[Load User from DB\nSet SecurityContext]

    LOAD --> ROLE{Controller annotation?}
    ROLE -->|@IsAuthenticated| OK[Any authenticated user — allow]
    ROLE -->|@IsAdmin| CHKADMIN{Has ADMIN role?}
    ROLE -->|@IsSuperAdmin| CHKSUPER{Has SUPER_ADMIN role?}
    ROLE -->|@IsModerator| CHKMOD{Has MODERATOR role?}
    ROLE -->|@IsAccountant| CHKACC{Has ACCOUNTANT role?}

    CHKADMIN -->|Yes| OK
    CHKADMIN -->|No| R403[403 Forbidden]
    CHKSUPER -->|Yes| OK
    CHKSUPER -->|No| R403
    CHKMOD -->|Yes| OK
    CHKMOD -->|No| R403
    CHKACC -->|Yes| OK
    CHKACC -->|No| R403

    OK --> CTRL[Controller → Service → Repository]
```

---

## 15. Complete API Endpoint Map

```mermaid
mindmap
  root((API v1))
    Auth
      POST /register
      POST /login
      GET /me
      POST /assign-roles
      GET /roles
      PUT /me/currency
    Expenses
      GET / paginated
      POST / create
      GET /{id}
      PUT /{id}
      DELETE /{id}
      GET /search
      GET /filter/category
      GET /filter/amount
      GET /filter/date-range
      POST /filter advanced
    Categories
      Admin /admin/categories
        POST
        PUT /{id}
        DELETE /{id}
      User /categories
        GET
        POST
        PUT /{id}
        DELETE /{id}
    Budgets
      POST
      GET
      GET /{id}/status
      PUT /{id}
      DELETE /{id}
    RecurringExpenses
      POST
      GET
      GET /{id}
      PUT /{id}
      DELETE /{id}
    Tags
      POST
      GET
      GET /{id}
      PUT /{id}
      DELETE /{id}
    Reports
      GET /summary
      GET /category-wise
      GET /date-wise
      POST /custom
      GET /trends
      GET /budget-performance
      GET /insights
      GET /top-expenses
      GET /export csv or pdf
    ExchangeRates
      GET / list rates
      GET /convert
      POST /sync ADMIN
    Notifications
      GET / paginated
      GET /unread
      GET /unread/count
      PUT /{id}/read
      PUT /read-all
      DELETE /{id}
    AuditLogs
      GET / ADMIN
      GET /me
      GET /user/{id} SUPER_ADMIN
      GET /entity/{type}/{id}
      GET /action/{action}
    Dashboard
      GET /
```
