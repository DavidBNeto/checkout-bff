# Glossary

This document defines the terminology used throughout the Subscription Checkout BFF project.

Terms should be used consistently across functional specifications, technical specifications, API contracts, architecture diagrams, code, and tests.

## A

### Asynchronous Checkout

Checkout processing model in which the API accepts a payment request and returns before the payment has been processed.

The frontend receives an operation identifier and retrieves the result through a status endpoint.

### Adapter

Infrastructure component responsible for implementing an application-facing interface and communicating with an external system or technology.

### API Contract

The agreed structure of an API operation, including its endpoint, request, response, status codes, and error formats.

## B

### Backend for Frontend (BFF)

A backend application designed to provide an API tailored to the needs of a particular frontend application.

In this project, the BFF orchestrates the subscription checkout experience and integrates with product and payment method services.

### BLOCK

Frontend error display type representing an error that interrupts the current checkout flow and requires user action.

## C

### Cart

A collection of selected subscription products created during pre-checkout.

A cart has a unique identifier and contains the product information and payment methods associated with the checkout session.

### Cart ID

Unique identifier assigned to a cart during pre-checkout.

### Checkout

The operation responsible for attempting to complete payment for a previously created cart.

### Checkout Operation

A specific execution or attempt to process a cart payment.

It is identified independently from the cart and has its own processing status.

### Checkout Status

The current state of a checkout operation.

Examples include `PENDING`, `PROCESSING`, `APPROVED`, `DECLINED`, and `FAILED`.

### Consumer

A component that receives and processes messages from a message broker.

### Correlation ID

Identifier used to associate logs and operations that belong to the same request or processing flow.

## D

### Dead-Letter Queue (DLQ)

A RabbitMQ queue used to hold messages that cannot be successfully processed according to the configured retry and failure policies.

### Domain Rule

A business constraint that determines whether an operation is valid.

Examples include preventing an already approved cart from being paid again.

## E

### Event

A record representing something that happened or needs to be processed in the application.

For example, a checkout payment request can generate an event for asynchronous processing.

### External Service

A system outside the BFF's ownership that provides information or functionality.

The Product Service and Payment Methods Service are external services in this project, even when simulated locally.

## F

### Frontend Error

A standardized error representation returned by the BFF so the frontend can display consistent messages and actions.

The contract includes `code`, `title`, `description`, and at least one `primaryButton`.

## I

### Idempotency

Property that ensures repeated execution of the same operation does not produce unintended duplicate effects.

In this project, payment processing must not result in multiple approvals for the same cart.

### INTERVENTION

Frontend error display type representing a warning or intervention that does not necessarily terminate the current flow.

## M

### Message Broker

Infrastructure responsible for receiving, routing, and delivering messages between producers and consumers.

RabbitMQ is the message broker selected for this project.

### Mock Service

A simulated implementation of an external service used to support local development and testing.

Mock services must still be consumed through the BFF's dedicated interfaces.

## O

### Outbox

A database-backed mechanism for recording events that must be published to a message broker.

### Transactional Outbox

Pattern in which application state changes and the corresponding outbox event are persisted within the same database transaction.

This prevents the application from committing a checkout operation without also recording the event required for asynchronous processing.

## P

### Payment

A simulated attempt to charge the amount associated with a cart using a selected payment method.

### Payment Method

A payment option made available to the frontend by the Payment Methods Service.

Examples include credit card, debit card, and Pix.

### Payment Worker

Background component responsible for consuming asynchronous payment requests and processing them.

### Pre-checkout

Operation that prepares a cart from the products selected by the frontend and retrieves the payment methods available for that cart.

### Product

A subscription offering available for purchase, identified by a slug and containing descriptive and pricing information.

### Product Snapshot

The product information preserved as part of a cart when it is created.

It ensures that the cart retains the information presented to the frontend even if the external product catalog changes later.

### Producer

A component that publishes messages to a message broker.

## R

### Retry

A mechanism that attempts to process an operation again after a failure.

Retries must be bounded and must not cause duplicate payment effects.

## S

### Synchronous Checkout

Checkout processing model in which the API waits for the payment simulation to finish and returns the final result directly in the HTTP response.

### Subscription

A product offering representing access to a service under a recurring commercial arrangement.

This project handles the purchase checkout only; recurring billing and subscription lifecycle management are out of scope.

## T

### 202 Accepted

HTTP response status indicating that a request has been accepted for processing, but processing has not necessarily completed.

It is used by the asynchronous checkout endpoint.

## U

### UUID

Universally Unique Identifier.

Used to identify carts and checkout operations.

