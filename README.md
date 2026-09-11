# Hamm
Hamm is a secure financial ledger API that handles transactional CRUD workflows with strict role-based access.

<img src="/hamm.gif" alt="Hamm GIF" width="300">

## Getting started

Requires Java 25, Docker, and Docker Compose.

```bash
# Run the API + PostgreSQL together
docker compose up --build

# Or run PostgreSQL only and the API from your IDE / Maven
docker compose up db
./mvnw spring-boot:run
```

Once running:
- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health check: http://localhost:8080/actuator/health

Try it:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"password123","fullName":"Jane Doe"}'
# copy the accessToken from the response, then:
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" -H "Authorization: Bearer <token>" \
  -d '{"currency":"USD"}'
```

### Running tests

```bash
./mvnw test
```

Unit tests and H2-backed integration tests always run. A separate Testcontainers
suite (`PostgresIntegrationTest`) exercises the same flows against a real
PostgreSQL container; it auto-skips if Docker isn't available locally, and runs
in CI where Docker is present.

### Configuration

All settings in `src/main/resources/application.yml` are overridable via
environment variables (see `docker-compose.yml` for the full list): `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION_MS`, `SERVER_PORT`. **Set a real `JWT_SECRET` before deploying
anywhere but local dev** — the default in `application.yml` is a placeholder.

## Tech stack

- **Java** runs the ledger reliably
- **Spring Boot** wires up the web server, DI container, and config
- **Spring Security** gates every request behind auth and roles
- **JWT** proves who you are without the server tracking sessions
- **PostgreSQL** stores the ledger with real transactional guarantees
- **Spring Data JPA** turns repository methods into SQL for us
- **Docker** packages the app so it runs the same way everywhere
- **GitHub Actions** builds and tests every push automatically

_The following was the original implementation plan for this project; the API
described above now implements it._

## Project setup
- Initialize Spring Boot project
- Configure Java 25
- Add dependencies:
  - Spring Web
  - Spring Data JPA
  - Spring Security
  - PostgreSQL Driver
  - Validation
  - JWT (jjwt)
  - Lombok
  - Spring Boot Test
- Configure application.yml
- Create Dockerfile
- Create docker-compose.yml with PostgreSQL
- Set up GitHub Actions CI pipeline

## Database design

### Create entities
- User
- Role (enum)
- Account
- Transaction
- LedgerEntry

### Relationships
- User -> Accounts (One-to-Many)
- User -> Transactions (One-to-Many)
- Transaction -> LedgerEntries (One-to-Many)
- Account -> LedgerEntries (One-to-Many)

### PostgreSQL constraints
- Positive transaction amounts
- Unique account numbers
- Unique transaction reference IDs
- Foreign keys
- Timestamp auditing

### Database indexes
- account_number
- transaction_reference
- account_id + created_at
- created_by + created_at

## Authentication & security

### JWT authentication
- User registration
- User login
- JWT generation
- JWT validation filter
- Stateless authentication
- Password hashing with BCrypt

### Spring Security
- Secure all endpoints by default
- Public auth endpoints
- JWT authentication filter
- Custom UserDetailsService

### Role-based authorization
Roles:
- CUSTOMER
- ACCOUNTANT
- AUDITOR
- ADMIN

Protect endpoints using:
- @PreAuthorize
- Method security
- Ownership validation

## Account management API

Implement endpoints:

- POST /api/v1/accounts
- GET /api/v1/accounts
- GET /api/v1/accounts/{id}
- PATCH /api/v1/accounts/{id}
- GET /api/v1/accounts/{id}/balance
- GET /api/v1/accounts/{id}/entries

Features:
- Create accounts
- View accounts
- Update account metadata
- View current balance
- View ledger history

## Transaction API

Implement:

- POST /transactions/deposit
- POST /transactions/withdraw
- POST /transactions/transfer
- GET /transactions
- GET /transactions/{id}

Business rules:
- Positive amounts only
- Sufficient funds
- Source != destination
- Active accounts only
- Atomic transactions
- Rollback on failure

---

## Ledger system

Implement double-entry accounting.

Every transfer should create:
- Debit entry
- Credit entry

Never modify ledger entries.
Corrections should create reversal transactions.

Store:
- Account
- Transaction
- Entry type
- Amount
- Balance after transaction
- Timestamp

## Service layer

Create services:

- AuthService
- UserService
- AccountService
- TransactionService
- LedgerService

TransactionService responsibilities:
- Validate request
- Check permissions
- Lock accounts
- Verify funds
- Update balances
- Create transaction
- Create ledger entries
- Commit atomically

Use @Transactional.

## Validation

Use Bean Validation annotations.

Validate:
- Amount > 0
- Required fields
- Currency length
- Description length

Business validation:
- No self-transfers
- No duplicate references
- Active accounts only
- Authorized ownership

## Exception handling

Create GlobalExceptionHandler.

Return consistent JSON errors.

Handle:
- ValidationException
- AccessDeniedException
- AccountNotFoundException
- InsufficientFundsException
- DuplicateReferenceException
- UnauthorizedException

## Concurrency

Prevent race conditions.

Implement:
- Pessimistic locking OR optimistic locking
- Atomic balance updates
- Transaction rollback on failure

## Testing

Write unit tests for:
- Authentication
- JWT validation
- Account creation
- Deposits
- Withdrawals
- Transfers
- Validation failures
- Authorization
- Insufficient funds
- Double-entry creation

Integration tests:
- PostgreSQL with Testcontainers
- Full REST API tests
- Security tests

## API documentation

Generate OpenAPI/Swagger docs.

Document:
- Authentication
- Request examples
- Response examples
- Error responses

## Docker

Create:
- Dockerfile
- docker-compose.yml

Containers:
- Spring Boot API
- PostgreSQL

Support one-command startup.

---

## CI/CD

GitHub Actions workflow:
- Build project
- Run unit tests
- Run integration tests
- Package application
- Build Docker image

## Nice-to-have features

- Pagination
- Filtering transactions
- Search by reference
- Audit logging
- Refresh tokens
- Rate limiting
- Account freezing
- Soft deletes for users
- Health checks
- Metrics with Spring Boot Actuator

## Suggested project structure

src/main/java/com/example/hamm

- auth/
- account/
- transaction/
- ledger/
- user/
- security/
- exception/
- config/
- dto/
- repository/
- service/
- controller/
- mapper/

## Definition of done

- JWT authentication working
- Role-based authorization enforced
- CRUD for accounts
- Deposit/withdraw/transfer endpoints
- Double-entry ledger implemented
- Atomic database transactions
- PostgreSQL indexes and constraints
- Validation on all requests
- Global exception handling
- Comprehensive unit and integration tests
- Dockerized application
- GitHub Actions CI passing
- Swagger/OpenAPI documentation available
