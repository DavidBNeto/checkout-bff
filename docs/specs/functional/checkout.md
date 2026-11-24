# Checkout Functional Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Business Analyst

---

## 1. Overview

This document defines the functional requirements for the Checkout operation (both synchronous and asynchronous) of the Subscription Checkout BFF.

---

## 2. Functional Requirements

### FR-CHK-001: Synchronous Checkout
**Priority**: Required  
**Status**: Confirmed

Process payment for a previously created cart and return the final result in the HTTP response.

**Endpoint**: `POST /api/v1/operations/checkout`

**Request Body**:
- `cartId`: UUID of the cart from pre-checkout
- `paymentMethod`: Payment method object with type and details (CREDIT_CARD, DEBIT_CARD, or PIX)

### FR-CHK-002: Asynchronous Checkout
**Priority**: Extension  
**Status**: Confirmed

Initiate payment processing asynchronously, return 202 Accepted with cart details, and allow status polling.

**Endpoint**: `POST /api/v1/operations/checkout/async`

**Request Body**: Same as synchronous checkout.

**Response**: 202 Accepted with cart details (status = PENDING)

### FR-CHK-003: Cart Validation
**Priority**: Required  
**Status**: Confirmed

Validate that the cart exists, is in PENDING status, and was created through pre-checkout.

### FR-CHK-004: Payment Method Validation
**Priority**: Required  
**Status**: Confirmed

Validate that the selected payment method type was offered during pre-checkout for that cart.

### FR-CHK-005: Duplicate Payment Prevention
**Priority**: Required  
**Status**: Confirmed

Prevent an already approved cart from being paid again (idempotency).

### FR-CHK-006: Payment Simulation
**Priority**: Required  
**Status**: Confirmed

Simulate payment processing with these rules:
- Card ending in `0002` → DECLINED
- Other valid cards → APPROVED
- Pix → APPROVED (returns simulated Pix code)

### FR-CHK-007: Payment Response
**Priority**: Required  
**Status**: Confirmed

Return appropriate response:
- Approved card: last 4 digits, amount, currency
- Approved Pix: Pix code
- Declined: standardized error contract

---

## 3. Business Rules

### BR-CHK-001: Cart Must Be Pending
Only carts with status `PENDING` can be checked out.

### BR-CHK-002: Payment Method Must Match Offer
The payment method type must be one of the methods returned in pre-checkout.

### BR-CHK-003: No Duplicate Approval
A cart with status `PAID` cannot be checked out again.

### BR-CHK-004: Card Declination Rule
Card numbers ending in `0002` are always declined.

### BR-CHK-005: Sensitive Data Not Returned
Full card numbers and CVV never returned in responses.

---

## 4. Acceptance Criteria

### AC-CHK-001: Successful Sync Checkout (Card)
**Given** a valid cart with credit card payment method  
**And** card does not end in 0002  
**When** POST /api/v1/operations/checkout  
**Then** 201 Created with APPROVED status and last 4 digits

### AC-CHK-002: Declined Payment (Card 0002)
**Given** a valid cart with card ending in 0002  
**When** POST /api/v1/operations/checkout  
**Then** 402 Payment Declined with standardized error

### AC-CHK-003: Successful Pix Payment
**Given** a valid cart with Pix payment method  
**When** POST /api/v1/operations/checkout  
**Then** 201 Created with APPROVED status and Pix code

### AC-CHK-004: Invalid Cart
**Given** a non-existent cart ID  
**When** POST /api/v1/operations/checkout  
**Then** 404 Cart Not Found

### AC-CHK-005: Already Paid Cart
**Given** a cart with status PAID  
**When** POST /api/v1/operations/checkout  
**Then** 409 Conflict with CART_ALREADY_PAID

### AC-CHK-006: Invalid Payment Method
**Given** a payment method not offered for the cart  
**When** POST /api/v1/operations/checkout  
**Then** 400/422 INVALID_PAYMENT_METHOD

### AC-CHK-007: Async Checkout Initiation
**Given** a valid cart  
**When** POST /api/v1/operations/checkout/async  
**Then** 202 Accepted with cart details (status = PENDING)

### AC-CHK-008: Async Status Polling
**Given** an async checkout in progress  
**When** GET /api/v1/operations/cart/{cartId}  
**Then** 200 OK with current status

---

## 5. Traceability

| Requirement | User Story | Acceptance Criteria | Business Rule |
|-------------|------------|---------------------|---------------|
| FR-CHK-001 | US-CHK-001 | AC-CHK-001..003 | BR-CHK-001..005 |
| FR-CHK-002 | US-CHK-002 | AC-CHK-007..008 | BR-CHK-001..005 |
| FR-CHK-003 | US-CHK-001 | AC-CHK-004 | BR-CHK-001 |
| FR-CHK-004 | US-CHK-001 | AC-CHK-006 | BR-CHK-002 |
| FR-CHK-005 | US-CHK-003 | AC-CHK-005 | BR-CHK-003 |
| FR-CHK-006 | US-CHK-001 | AC-CHK-001..003 | BR-CHK-004 |
| FR-CHK-007 | US-CHK-001 | AC-CHK-001..003 | BR-CHK-005 |