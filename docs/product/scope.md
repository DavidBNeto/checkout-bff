# Project Scope

## 1. Purpose

This document defines the functional and technical boundaries of the Subscription Checkout BFF project.

It distinguishes the requirements of the original technical challenge from planned extensions intended to demonstrate additional backend engineering capabilities.

## 2. In Scope

### 2.1 Pre-checkout

Implement:

`POST /operations/pre-checkout`

Responsibilities:

- Receive a list of products (each with a slug) selected by the frontend.
- Retrieve product information from the Product Service.
- Create a checkout cart with a unique identifier.
- Return the cart identifier, product details (slug, name, description, price, currency), total amount, and currency.
- Reject invalid products using the standardized frontend error contract.

The product catalog must not be hardcoded in the BFF controller.

Suggested product catalog:

| Product | Monthly price |
|---|---:|
| Basic | BRL 19.90 |
| Premium | BRL 39.90 |
| Family | BRL 59.90 |

**Note**: Payment methods are not returned in pre-checkout. They are retrieved separately by the frontend at checkout time and validated against the Payment Methods Service.

### 2.2 Synchronous Checkout

Implement:

`POST /operations/checkout`

Responsibilities:

- Receive a cart identifier and selected payment method.
- Validate that the cart exists and was created through pre-checkout.
- Validate that the selected payment method is active in the Payment Methods Service.
- Simulate payment processing.
- Return the expected successful payment response or a standardized frontend error.
- Prevent an already approved cart from being paid again.

Payment simulation rules:

- A card ending in `0002` must be declined.
- Other valid simulated card payments may be approved.
- Pix payments must not require card information.
- Approved card payments return only the last four digits.
- Approved Pix payments return a simulated Pix code.

No real payment gateway integration is included.

### 2.3 Asynchronous Checkout

This is a planned extension beyond the original challenge.

Implement:

`POST /operations/checkout/async`

Responsibilities:

- Accept the same essential checkout information as the synchronous operation.
- Validate the cart and selected payment method against the Payment Methods Service.
- Create a checkout operation in the `PENDING` state.
- Persist the operation and its corresponding event reliably.
- Return HTTP `202 Accepted` without waiting for payment processing.
- Process the payment asynchronously through RabbitMQ.

Implement:

`GET /operations/checkouts/{checkoutId}`

Responsibilities:

- Retrieve the status of an asynchronous checkout operation.
- Return the current persisted state and relevant payment information.
- Allow the frontend to identify whether processing is pending, approved, declined, or failed.

Both checkout modalities must share the same business rules and payment simulation logic.

### 2.4 External Services

Implement dedicated interfaces and adapters for:

- Product Service.
- Payment Methods Service.

The services may be simulated locally, but the BFF must interact with them through their respective interfaces.

### 2.5 Persistence

Use PostgreSQL to persist the application state required for:

- Checkout carts.
- Product snapshots associated with carts (slug, name, description, price, currency).
- Checkout operations.
- Payment attempts.
- Events awaiting publication through the transactional outbox.

The persisted cart information must remain consistent with the information presented to the frontend during pre-checkout.

**Note**: Payment methods are not persisted with the cart; they are retrieved at checkout time for validation.

### 2.6 Caching

Use Redis to cache responses from:

- Product Service.

Cache behavior must not compromise the consistency of existing carts.

### 2.7 Asynchronous Messaging

Use RabbitMQ for asynchronous checkout processing.

The implementation should demonstrate:

- Event publication.
- Message consumption.
- Consumer idempotency.
- Retry handling.
- Dead-letter queue (DLQ).
- Transactional outbox pattern.

Sensitive card information must never be included in published events or persisted outbox records.

### 2.8 Automated Testing

Implement automated tests covering:

- Successful pre-checkout.
- Invalid products.
- Successful synchronous checkout.
- Declined payment.
- Invalid or missing cart.
- Invalid payment method.
- Duplicate payment attempts.
- Successful asynchronous checkout.
- Asynchronous processing failures.
- Duplicate message delivery.
- Relevant persistence and messaging integration scenarios.

Use JUnit and Mockito, with integration testing tools where appropriate.

### 2.9 Local Execution and Documentation

Provide:

- Docker-based local execution.
- PostgreSQL, Redis, and RabbitMQ dependencies.
- Application configuration and environment variables.
- Database migrations.
- API documentation.
- README with installation and execution instructions.
- Examples for simulating successful and declined payments.
- Architecture and database diagrams.

## 3. Out of Scope

The following are explicitly excluded:

- Real payment gateway integration.
- Real financial transactions.
- Actual subscription provisioning or recurring billing.
- Subscription cancellation and renewal management.
- User registration and authentication.
- Customer account management.
- A production frontend application.
- Ownership of the product catalog.
- Ownership of the payment method catalog.
- Production deployment to a cloud provider.
- Real card tokenization or PCI-compliant payment processing.

These exclusions may be reconsidered if future requirements justify expanding the project.

## 4. Functional Boundaries

| Capability | Responsible component |
|---|---|
| Product catalog | Product Service |
| Available payment methods | Payment Methods Service |
| Checkout orchestration | Checkout BFF |
| Cart lifecycle | Checkout BFF |
| Payment simulation | Payment Processing component |
| Asynchronous execution | Payment Worker |
| Persistent checkout state | PostgreSQL |
| External response caching | Redis |
| Asynchronous event delivery | RabbitMQ |

## 5. API Evolution Strategy

The project will demonstrate two checkout processing models.

### Phase 1: Synchronous API

The original challenge contract is implemented first.

`POST /operations/checkout`

The frontend receives the final payment result directly in the HTTP response.

### Phase 2: Asynchronous API

An additional processing model is introduced without replacing the synchronous endpoint.

`POST /operations/checkout/async`

The frontend receives HTTP `202 Accepted` and a checkout identifier. It can subsequently query the operation status.

`GET /operations/checkouts/{checkoutId}`

Both endpoints share the same domain rules and persistent state.

The asynchronous extension must not change the expected behavior of the original synchronous endpoint.

## 6. Non-Functional Requirements

### Reliability

- Persist checkout state consistently.
- Avoid duplicate payment approval.
- Ensure that persisted asynchronous events are eventually published.
- Handle duplicate message delivery safely.

### Security

- Never persist full card numbers or CVV values.
- Never include sensitive payment information in logs, events, or outbox records.
- Return only payment information necessary for the frontend.

### Maintainability

- Separate API, application, domain, and infrastructure responsibilities.
- Keep external integrations behind dedicated interfaces.
- Avoid unnecessary coupling between synchronous and asynchronous processing.

### Testability

- Business rules must be testable independently of infrastructure.
- Integration tests must validate relevant persistence and messaging behavior.
- Tests must not depend on real external payment services.

### Observability

- Provide structured application logs.
- Expose health and readiness information.
- Track asynchronous processing and failures.
- Support correlation of requests and asynchronous operations.

## 7. Scope Priorities

| Priority | Deliverable |
|---|---|
| Required | Pre-checkout API |
| Required | Synchronous checkout API |
| Required | Standardized success and error contracts |
| Required | Checkout business rules |
| Required | External service interfaces |
| Required | Automated tests |
| Extension | PostgreSQL persistence |
| Extension | Redis caching |
| Extension | Asynchronous checkout |
| Extension | RabbitMQ and transactional outbox |
| Extension | Retry and DLQ |
| Extension | Docker-based execution |
| Extension | Observability and expanded integration tests |

