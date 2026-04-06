# IMS User Service

Authentication and User Management service for Inventory Management System(STOCKOX).

## Tech Stack
- Java 17
- Spring Boot 3.2.4
- PostgreSQL
- Redis
- JWT Authentication

## Setup

### Prerequisites
- Java 17+
- PostgreSQL running on port 5432
- Redis running on port 6379

### Run locally
```bash
# Create database
psql -U postgres -c "CREATE DATABASE stockox_users_dev;"

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### API Base URL
`http://localhost:8081/api/v1`

## Branches
- `main` — production ready code
- `dev` — development branch
- `qa` — testing branch