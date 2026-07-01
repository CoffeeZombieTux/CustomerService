# Customer Loyalty & Account Service

![CI](https://github.com/CoffeeZombieTux/CustomerService/actions/workflows/ci.yml/badge.svg)

A REST API for managing customer accounts, bonus balances, and consent agreements. Serves external clients and internal services.

## Architecture

```
  ┌──────────┐        ┌─────────────────┐   ┌──────────────────┐
  │  Client  │        │   Admin Panel   │   │Internal Services │
  └────┬─────┘        └────────┬────────┘   └────────┬─────────┘
       │ JWT                   │ JWT                 │ API Key
       ▼                       │ + ADMIN role        │
  ┌────────────┐               └──────────┬──────────┘
  │ API Gateway│                          │ internal network
  └─────┬──────┘                          │
        └───────────────┬─────────────────┘
                        ▼
               ┌──────────────────┐
               │  CustomerService │
               └────────┬─────────┘
                        │
               ┌────────▼─────────┐
               │    PostgreSQL    │
               └──────────────────┘
```

CustomerService sits behind an API Gateway and exposes three API surfaces, each secured independently:

| Surface | Path prefix | Auth |
|---|---|---|
| Customer | `/api/v1/auth/**`, `/api/v1/customers/**`, `/api/v1/bonus`, `/api/v1/addresses/**` | JWT Bearer |
| Admin | `/api/v1/admin/**` | JWT Bearer + `ADMIN` role |
| Internal | `/api/v1/internal/**` | Shared API key (`X-Internal-Api-Key` header) |

The gateway is responsible for TLS termination, CORS, rate limiting, and security headers. The internal API should additionally be restricted at the network level (e.g. Kubernetes NetworkPolicy or firewall rules).

---

## Auth & Sessions

### Registration flow
1. Customer submits registration form with mandatory agreements
2. Account is created (disabled), activation email sent
3. Customer clicks the link → account enabled
4. Customer logs in → receives access token + refresh token

### Activation flow

Two-step activation is used to prevent email security scanners and browser prefetch from consuming the activation token before the user acts.

1. The activation email contains a link to the **frontend** confirmation page (`ACTIVATION_BUTTON_URL?token=<uuid>`)
2. The frontend calls `GET /api/v1/auth/activate?token=<uuid>` — validates the token without any side effects
3. If valid, the frontend shows a "Confirm your email" button
4. The user clicks the button → frontend calls `POST /api/v1/auth/activate` with the token
5. The account is enabled, the token is consumed, and the user is redirected to `ACTIVATION_SUCCESS_URL`

If the token is missing or expired at any step, the user is redirected to `ACTIVATION_FAIL_URL`.

### Token model

| Token | Type | TTL | Notes |
|---|---|---|---|
| Access token | JWT | 15 min | Claims: `sub` (customerId), `email`, `role`, `sessionId` |
| Refresh token | Hashed 256-bit secret | 7 days | Single-use with rotation; stored as SHA-256 hash |

- Max **5 concurrent sessions** per customer
- Changing password immediately invalidates all sessions
- Expired tokens purged nightly at 03:00

---

## Bonus Programme

| Rule | Detail |
|---|---|
| Earn idempotency | UUID key required; duplicate within 24 h → 409 |

Credits are unitless — their meaning is defined by the consuming service. Internal services credit and debit balances via the internal API. Balance updates use pessimistic locking to prevent race conditions under concurrent requests.

---

## Agreement Audit Log

Every consent change is written as a new immutable record — the full history is preserved. `GET .../agreements/active` returns only the latest accepted consent per type.

---

## Data Model

```
Customer
  ├── BonusAccount         (1:1)
  │     └── CreditTransaction  (1:N)
  ├── Address              (1:N)
  ├── CustomerAgreement    (1:N, consent audit log)
  ├── RefreshToken         (1:N, max 5 per customer)
  └── ActivationToken      (1:N)
```

---

## Setup

### Docker (recommended)

```bash
cp .env.example .env   # fill in the values
docker compose up --build
```

App starts at `http://localhost:8080`. PostgreSQL data is persisted in a named volume.

### Prerequisites (local)
- Java 21
- PostgreSQL 17+ at `localhost:5432` (database: `customerdb`)

### Environment variables

Copy `.env.example` to `.env` and fill in the values.

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 key (≥ 256 bits) |
| `ACTIVATION_BUTTON_URL` | Frontend page with the confirmation button (step 1 of two-step activation) |
| `ACTIVATION_SUCCESS_URL` | Redirect after successful activation |
| `ACTIVATION_FAIL_URL` | Redirect after failed or expired activation |
| `INTERNAL_API_KEY` | Shared secret for inter-service calls |

**IntelliJ IDEA:** Edit Run Configuration → Environment variables → load from `.env` file.

**Terminal:**
```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

### Commands

```bash
./mvnw spring-boot:run       # run the app
./mvnw clean package         # build
./mvnw compile               # fast compile check
./mvnw test                  # run test suite
```

---

## Tests

44 tests, zero mocks at the database layer.

| Layer | Class | Tests |
|---|---|---|
| Service | `AuthServiceTest` | 16 |
| Service | `CustomerServiceTest` | 7 |
| Service | `BonusServiceTest` | 6 |
| Controller | `AuthControllerTest` | 7 |
| Controller | `CustomerControllerTest` | 4 |
| Security | `JwtServiceTest` | 4 |

**Service tests** are pure Mockito unit tests — no Spring context, fast feedback.

**Controller tests** use `@SpringBootTest` with H2 in PostgreSQL-compatibility mode and `MockMvc`. Real security filter chain is applied, so unauthenticated requests return 401 as expected in production.

**What's covered:** registration (success, duplicate email, validation failure), login (success, bad credentials), token refresh, logout, profile CRUD, bonus credit/redeem (including idempotency and insufficient balance), JWT claims, token expiry.

### Seeding an admin account

Admin accounts are created directly in the database — there is no registration endpoint.

```sql
INSERT INTO customers (role, email, password, first_name, last_name, phone, enabled, created_at, updated_at)
VALUES ('ADMIN', 'admin@example.com', '<bcrypt-hash>', 'Admin', 'User', '+10000000000', true, now(), now());
```

Generate a BCrypt hash (cost 10):

```bash
htpasswd -bnBC 10 "" 'yourpassword' | tr -d ':\n'
```

---

## Tech Stack

Spring Boot 4.0 · Java 21 · Spring Security 6 · JJWT 0.12 · PostgreSQL · Flyway · Jakarta Validation
