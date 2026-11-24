# Product Vision

## 1. Overview

Build a Subscription Checkout Backend for Frontend (BFF) responsible for orchestrating the subscription checkout experience between a frontend application and external services.

The application provides the frontend with the information required to display available subscription products, payment methods, and checkout results. It coordinates the checkout lifecycle while keeping product and payment method ownership outside the BFF.

The project will also demonstrate the evolution of a checkout API from a synchronous processing model to an asynchronous architecture, without breaking the original synchronous contract.

## 2. Problem Statement

A subscription checkout frontend needs a reliable backend interface that simplifies communication with multiple services and provides predictable responses throughout the payment journey.

Without a dedicated BFF, frontend applications may need to understand multiple backend integrations, deal with inconsistent response formats, and implement unnecessary orchestration logic.

The BFF addresses this by:

- Aggregating product and payment method information.
- Creating and managing checkout carts.
- Validating checkout requests and business rules.
- Simulating payment processing.
- Providing stable success and error contracts.
- Supporting both synchronous and asynchronous checkout processing.

## 3. Product Vision

Provide a well-structured, reliable, and testable checkout BFF that demonstrates sound backend engineering practices, clear API contracts, and the ability to evolve an application without unnecessarily coupling its frontend consumers to internal implementation details.

## 4. Target Consumers

**Primary consumer:**
- A frontend application responsible for the subscription purchase experience.

**External dependencies:**
- Product Service: provides subscription product information.
- Payment Methods Service: provides available payment methods.

These services may be simulated for this project, but the BFF must consume them through dedicated interfaces rather than directly accessing their underlying data.

## 5. Product Principles

### Frontend-oriented API

The BFF should expose only the information and operations required by the frontend, hiding unnecessary integration details.

### Stable contracts

Success and error responses must follow predictable structures. Internal implementation changes should not require unnecessary frontend changes.

### Clear ownership

The BFF orchestrates checkout but is not the source of truth for the product catalog or available payment methods.

### Security by design

Sensitive payment information must never be persisted unnecessarily. Full card numbers and CVV values must not be stored.

### Evolvability

The application should support synchronous and asynchronous checkout processing through shared business rules and a consistent domain model.

### Testability

Business rules, integrations, and application workflows should be independently testable.

## 6. Success Criteria

The project will be considered successful when it demonstrates:

- A working pre-checkout and checkout flow.
- Correct integration with product and payment method services.
- Stable API contracts for successful and failed operations.
- Correct enforcement of checkout business rules.
- Protection against duplicate payment approval.
- Automated unit and integration tests.
- Reliable asynchronous processing.
- Clear documentation and reproducible local execution.
- A documented architectural evolution from synchronous to asynchronous checkout.

## 7. Technical Direction

The project is expected to use:

- Java and Spring Boot.
- PostgreSQL for persistent application state.
- RabbitMQ for asynchronous payment processing.
- Redis for caching external service responses.
- JUnit and Mockito for automated testing.
- Docker for local execution and environment reproducibility.

These technologies are implementation choices for demonstrating the project's engineering goals, not requirements imposed by the original challenge.

