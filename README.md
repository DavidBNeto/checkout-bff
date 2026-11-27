# Checkout BFF

A multi-service checkout backend built with Spring Boot 4.2.0, following Clean Architecture and Spec-Driven Development (SDD).

## Architecture Overview

This project consists of three independent Spring Boot services orchestrated via Docker Compose:

```
checkout-bff/
├── docker-compose.yml              # Root orchestration
├── init-multiple-dbs.sh            # PostgreSQL multi-database init
└── services/
    ├── checkout-bff/               # BFF Service (Port 8080)
    ├── product-service/            # Product Service (Port 8081)
    └── payment-service/            # Payment Service (Port 8083)
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **checkout-bff** | 8080 | Backend-for-Frontend orchestrating pre-checkout flow |
| **product-service** | 8081 | Product catalog with Redis caching |
| **payment-service** | 8083 | Payment methods & payment processing with Redis caching |

### Shared Infrastructure
- **PostgreSQL 18** - Separate databases per service
- **Redis 8** - Caching for all services
- **RabbitMQ 4** - Async messaging (checkout-bff publishes order events, payment-service consumes and processes payments)

---

## Technology Stack

- **Java 21** (target), **Spring Boot 4.2.0**
- **Spring Data JPA** - PostgreSQL persistence
- **Spring AMQP** - RabbitMQ messaging (checkout-bff & payment-service)
- **Spring Data Redis** - Redis caching
- **Flyway** - Database migrations
- **MapStruct 1.6.3** - Object mapping
- **Lombok 1.18.38** - Boilerplate reduction
- **SpringDoc OpenAPI 3.2.1** - API documentation
- **Testcontainers** - Integration testing
- **Maven** - Build tool

## Key Architecture Decisions

### Async Checkout Flow
- **checkout-bff** publishes `OrderCreatedEvent` to RabbitMQ outbox when async checkout is initiated
- **payment-service** consumes `OrderCreatedEvent`, processes payment, publishes `PaymentProcessedEvent`
- **checkout-bff** consumes `PaymentProcessedEvent` and updates checkout operation status

### Outbox Pattern (Both Services)
- Transactional outbox with JSONB payload storage
- `@JdbcTypeCode(SqlTypes.JSON)` for proper JSONB column mapping
- Background relay publishes events to RabbitMQ

### Payment Processing Responsibility
- Moved from checkout-bff to payment-service
- checkout-bff only orchestrates pre-checkout and receives payment results
- payment-service owns payment logic (approval/decline simulation, PIX generation, etc.)

---

## Prerequisites

- Java 21+
- Maven 3.9+ (wrapper provided)
- Docker & Docker Compose

---

## Getting Started

### 1. Start All Services with Docker Compose

```bash
docker-compose up --build
```

This starts:
- PostgreSQL on port 5432 (databases: `checkout_bff`, `product_service`, `payment_service`)
- RabbitMQ on port 5672 (Management UI on 15672)
- Redis on port 6379
- All three Spring Boot services

### 2. Run Individual Services Locally

Each service has its own Maven wrapper:

```bash
# Terminal 1 - Product Service
cd services/product-service
./mvnw spring-boot:run

# Terminal 2 - Payment Service
cd services/payment-service
./mvnw spring-boot:run

# Terminal 3 - Checkout BFF
cd services/checkout-bff
./mvnw spring-boot:run
```

> **Note**: Local runs require infrastructure (PostgreSQL, Redis, RabbitMQ) running. Use `docker-compose up -d postgres redis rabbitmq` for infrastructure only.

### 3. Verify Services

| Service | Health Check | Swagger UI |
|---------|-------------|------------|
| Checkout BFF | http://localhost:8080/actuator/health | http://localhost:8080/swagger-ui.html |
| Product Service | http://localhost:8081/actuator/health | http://localhost:8081/swagger-ui.html |
| Payment Service | http://localhost:8083/actuator/health | http://localhost:8083/swagger-ui.html |

---

## API Endpoints

### Checkout BFF (Port 8080)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/operations/pre-checkout` | Create pre-checkout with cart, products & payment methods |

**Pre-Checkout Request:**
```bash
curl -X POST http://localhost:8080/api/v1/operations/pre-checkout \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"slug": "product-a", "quantity": 2},
      {"slug": "product-b", "quantity": 1}
    ],
    "paymentMethodId": "credit-card"
  }'
```

### Product Service (Port 8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/products` | List products (query: `?slugs=slug1,slug2&active=true`) |
| GET | `/api/v1/products/{slug}` | Get product by slug |

### Payment Service (Port 8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/payment-methods` | List active payment methods (`?active=true`) |
| GET | `/api/v1/payment-methods/{name}` | Get payment method by name |

> **Note**: Payment processing is now handled internally by payment-service via RabbitMQ. The checkout-bff initiates async checkout (publishes `OrderCreatedEvent`), payment-service consumes and processes it, then publishes `PaymentProcessedEvent` back to checkout-bff.

---

## Configuration

### Environment Variables (Docker Compose)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Active Spring profile |
| `SPRING_DATASOURCE_URL` | Service-specific | PostgreSQL JDBC URL |
| `SPRING_DATA_REDIS_URL` | `redis://redis:6379` | Redis URL |
| `SPRING_RABBITMQ_HOST` | `rabbitmq` | RabbitMQ host |
| `PRODUCT_SERVICE_URL` | `http://product-service:8081` | Product Service URL (BFF) |
| `PAYMENT_METHODS_SERVICE_URL` | `http://payment-service:8083` | Payment Service URL (BFF) |

### Cache TTL (minutes)

| Cache | TTL |
|-------|-----|
| Products | 10 |
| Payment Methods | 15 |
| Cart | 30 |

---

## Database Schema

Each service has its own database with Flyway migrations:

**Product Service** (`product_service`):
- `product` - Product catalog (slug, name, description, price, currency, active)

**Payment Service** (`payment_service`):
- `payment_method` - Payment methods (name, normalized_name, type, active)
- `outbox` - Transactional outbox for publishing PaymentProcessedEvent

**Checkout BFF** (`checkout_bff`):
- `shopping_cart` - Cart with public_id, price, status, payment_data_id
- `cart_item` - Items with product reference, quantity, price
- `product` - Local product cache (synced from product-service)
- `payment_method` - Local payment method cache (synced from payment-service)
- `outbox` - Transactional outbox for publishing OrderCreatedEvent, CartUpdatedEvent
- `checkout_operation` - Async checkout operations with payment status tracking

---

## Clean Architecture Structure (per service)

```
src/main/java/com/checkout/{service}/
├── domain/                 # Enterprise Business Rules
│   ├── entity/            # JPA Entities
│   ├── repository/        # Repository Interfaces
│   ├── valueobject/       # Value Objects (Money, etc.)
│   └── exception/         # Domain Exceptions
├── application/           # Application Business Rules
│   ├── dto/               # Data Transfer Objects
│   ├── service/           # Use Case Implementations
│   ├── port/              # Ports (Interfaces)
│   └── mapper/            # MapStruct Mappers
├── infrastructure/        # Frameworks & Drivers
│   ├── persistence/       # JPA Repositories & Implementations
│   ├── cache/             # Redis Adapters
│   ├── messaging/         # RabbitMQ Consumers & Outbox Publisher
│   │   ├── event/         # Domain Events (OrderCreatedEvent, PaymentProcessedEvent)
│   │   └── ...            # OutboxMessagePublisher, Consumers
│   ├── client/            # External Service Clients (BFF only)
│   └── config/            # Infrastructure Configuration
└── presentation/          # Interface Adapters
    ├── controller/        # REST Controllers
    ├── dto/               # Request/Response DTOs
    └── mapper/            # Presentation Mappers
```

### Service-Specific Infrastructure

**checkout-bff** infrastructure:
- `messaging`: OutboxMessagePublisher (CartUpdatedEvent, OrderCreatedEvent), PaymentRequestConsumer (consumes PaymentProcessedEvent)
- `client`: ProductServiceClient, PaymentMethodsServiceClient

**payment-service** infrastructure:
- `messaging`: OutboxMessagePublisher (PaymentProcessedEvent), OrderCreatedEventConsumer (consumes OrderCreatedEvent)
- No external clients (owns payment methods & processing)

---

## Testing

```bash
# All services
for dir in services/*/; do
  cd "$dir" && ./mvnw test && cd ../..
done

# Individual service
cd services/checkout-bff && ./mvnw test
cd services/product-service && ./mvnw test
cd services/payment-service && ./mvnw test
```

Tests use Testcontainers for PostgreSQL, Redis, and RabbitMQ.

---

## Building

```bash
# Build all services
for dir in services/*/; do
  cd "$dir" && ./mvnw clean package -DskipTests && cd ../..
done
```

Docker images are built via `docker-compose build`.

---

## Development

### Code Quality
- **MapStruct** for DTO mapping (annotation processor)
- **Lombok** for boilerplate reduction
- **Spring Boot 4** with Jakarta EE 10

### Adding a New Service
1. Create `services/new-service/` with Spring Boot project
2. Add `Dockerfile` and `application-docker.properties`
3. Add service to `docker-compose.yml`
4. Add database to `init-multiple-dbs.sh` (POSTGRES_MULTIPLE_DATABASES)

---

## License

Internal use only.