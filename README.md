# Checkout BFF

Backend for Frontend service for checkout operations, built with Spring Boot 3, following Clean Architecture principles.

## Architecture

```
src/main/java/com/checkout/bff/
├── domain/                 # Domain Layer (Enterprise Business Rules)
│   ├── entity/            # JPA Entities
│   ├── repository/        # Repository Interfaces (Domain)
│   ├── service/           # Domain Services
│   ├── event/             # Domain Events
│   ├── valueobject/       # Value Objects
│   └── exception/         # Domain Exceptions
│
├── application/           # Application Layer (Application Business Rules)
│   ├── usecase/           # Use Cases / Interactors
│   ├── dto/               # Data Transfer Objects
│   ├── service/           # Application Services (Use Case Implementations)
│   ├── port/              # Ports (Interfaces)
│   │   ├── input/         # Input Ports (Use Case Interfaces)
│   │   └── output/        # Output Ports (Repository/External Interfaces)
│   ├── mapper/            # MapStruct Mappers
│   └── event/             # Application Events
│
├── infrastructure/        # Infrastructure Layer (Frameworks & Drivers)
│   ├── persistence/       # JPA Repositories
│   ├── repositoryimpl/    # Repository Implementations
│   ├── messaging/         # RabbitMQ Configuration & Publishers
│   ├── cache/             # Redis Configuration & Adapters
│   ├── config/            # Infrastructure Configuration
│   └── external/          # External Service Clients
│
└── presentation/          # Presentation Layer (Interface Adapters)
    ├── controller/        # REST Controllers
    ├── request/           # Request DTOs
    ├── response/          # Response DTOs
    ├── mapper/            # Presentation Mappers
    └── config/            # Presentation Configuration
```

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.5**
- **Spring Data JPA** - PostgreSQL persistence
- **Spring AMQP** - RabbitMQ messaging
- **Spring Data Redis** - Redis caching
- **Flyway** - Database migrations
- **MapStruct** - Object mapping
- **Lombok** - Boilerplate reduction
- **SpringDoc OpenAPI** - API documentation
- **Testcontainers** - Integration testing

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for local infrastructure)

## Getting Started

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- RabbitMQ on port 5672 (Management UI on 15672)
- Redis on port 6379

### 2. Run Application

```bash
./mvnw spring-boot:run
```

Or build and run:

```bash
./mvnw clean package
java -jar target/checkout-bff-1.0.0-SNAPSHOT.jar
```

### 3. Access API

- **API Base URL**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Actuator Health**: http://localhost:8080/actuator/health

## API Endpoints

### Shopping Cart

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/carts` | Create new shopping cart |
| GET | `/carts/{cartId}` | Get shopping cart by ID |
| POST | `/carts/{cartId}/items` | Add item to cart |

### Example Requests

**Create Cart:**
```bash
curl -X POST http://localhost:8080/api/v1/carts \
  -H "Content-Type: application/json" \
  -d '{
    "paymentDataId": 1,
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ]
  }'
```

**Get Cart:**
```bash
curl http://localhost:8080/api/v1/carts/1
```

**Add Item:**
```bash
curl -X POST http://localhost:8080/api/v1/carts/1/items \
  -H "Content-Type: application/json" \
  -d '{"productId": 3, "quantity": 1}'
```

## Configuration

Environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/checkout_db` | PostgreSQL JDBC URL |
| `POSTGRES_USER` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USER` | `guest` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(empty)* | Redis password |

## Database Schema

Based on the domain model:

- **payment_method** - Payment methods (credit card, PIX, boleto, etc.)
- **payment_data** - Customer payment information linked to method
- **shopping_cart** - Cart with payment data, price, status
- **cart_item** - Items in cart with product reference and quantity
- **product** - Product catalog with price and currency

## Messaging

Events published to RabbitMQ exchange `checkout.exchange`:

| Routing Key | Event | Description |
|-------------|-------|-------------|
| `order.created` | `OrderCreatedEvent` | New order from cart |
| `payment.processed` | `PaymentProcessedEvent` | Payment completion |
| `cart.updated` | `CartUpdatedEvent` | Cart modifications |

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests (requires testcontainers profile)
./mvnw test -Ptestcontainers
```

## Building

```bash
./mvnw clean package -DskipTests
```

## License

Internal use only.
