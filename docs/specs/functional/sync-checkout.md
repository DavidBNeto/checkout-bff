# Sync Checkout Functional Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Business Analyst

---

## 1. Overview

### 1.1 Purpose
This document defines the functional requirements, business rules, user stories, and acceptance criteria for the **Synchronous Checkout** operation of the Subscription Checkout BFF. The sync checkout processes payment for a previously created cart and returns the final result in the HTTP response.

### 1.2 Scope
The Synchronous Checkout operation:
- Accepts a cart ID and payment method with payment data
- Validates the cart and payment method
- Simulates payment processing according to defined rules
- Returns the payment result (approved/declined) in the same HTTP response
- Returns the products that were paid for (slug, name, price)
- Updates cart status and publishes domain events

**Out of Scope**:
- Asynchronous checkout (covered in [Checkout Specification](checkout.md#fr-chk-002))
- Real payment gateway integration (payment is simulated)
- Payment method management (owned by Payment Methods Service)
- Technical implementation details (see [Technical API Contracts](../technical/api-contracts.md#3-related-endpoints) and [Architecture](../technical/architecture.md))

### 1.3 Related Documents
- [Product Vision](../../product/vision.md)
- [Project Scope](../../product/scope.md#22-checkout)
- [Glossary](../../product/glossary.md)
- [Pre-Checkout Specification](pre-checkout.md)
- [Payment Specification](payment.md)
- [Error Handling Specification](error-handling.md)
- [Technical API Contracts](../technical/api-contracts.md#3-synchronous-checkout)
- [Architecture Specification](../technical/architecture.md)

---

## 2. Functional Requirements

### FR-SYNC-001: Process Synchronous Checkout
**Priority**: Required  
**Status**: Confirmed

Process payment for a previously created cart and return the final result in the HTTP response.

**Endpoint**: `POST /api/v1/operations/checkout`

**Request Body Schema**:
```json
{
  "cartId": "uuid",
  "paymentMethod": {
    "type": "CREDIT_CARD|DEBIT_CARD|PIX",
    "card": {  // Required for CREDIT_CARD and DEBIT_CARD
      "holderName": "string",
      "number": "string",
      "expiry": "MM/YY",
      "cvv": "string"
    },
    "pixPayer": {  // Required for PIX
      "name": "string",
      "cpf": "string"  // 11 digits, validated with check digits
    }
  }
}
```

---

### FR-SYNC-002: Cart Existence and Status Validation
**Priority**: Required  
**Status**: Confirmed

Validate that the cart exists, is in `PENDING` status, and was created through pre-checkout.

**Validation Rules**:
- Cart must exist in database → `INVALID_CART` (404) if not
- Cart status must be `PENDING` → `ALREADY_PAID` (409) if paid
- Cart must have items → `INVALID_REQUEST` (400) if empty

---

### FR-SYNC-003: Payment Method Validation
**Priority**: Required  
**Status**: Confirmed

Validate that the selected payment method type was offered during pre-checkout for that cart.

**Validation Rules**:
- Payment method type must be one of: CREDIT_CARD, DEBIT_CARD, PIX
- Payment method must have been available at pre-checkout time
- Card data required for CREDIT_CARD and DEBIT_CARD types

**Error**: `INVALID_PAYMENT_DETAILS` (400/422) or `PAYMENT_DECLINED` (402)

---

### FR-SYNC-004: Duplicate Payment Prevention (Idempotency)
**Priority**: Required  
**Status**: Confirmed

Prevent an already approved cart from being paid again.

**Mechanism**:
- Client provides `Idempotency-Key` header (recommended)
- Server checks if cart status is `PAID` → `ALREADY_PAID` (409)
- Server checks if same idempotency key is in-flight → `IDEMPOTENCY_CONFLICT` (409)

---

### FR-SYNC-005: Payment Simulation
**Priority**: Required  
**Status**: Confirmed

Simulate payment processing according to [Payment Specification](payment.md#3-payment-simulation-rules).

**Card Payment Rules**:
| Card Pattern | Result | Details |
|--------------|--------|---------|
| Ends in `0002` | DECLINED | Simulates insufficient funds |
| Ends in `0001` | APPROVED | Simulates successful payment |
| Other valid Luhn | APPROVED | Generic approval |
| Invalid Luhn | DECLINED | Invalid card number |

**Pix Payment Rules**:
| Condition | Result | Details |
|-----------|--------|---------|
| Valid PIX payer (name + valid CPF) | APPROVED | Returns simulated Pix copy-paste code |
| Invalid PIX payer (missing name or invalid CPF) | DECLINED | Invalid payer data |

---

### FR-SYNC-006: Payment Response Construction
**Priority**: Required  
**Status**: Confirmed

Return appropriate response based on payment method and result. **All approved responses must include the products that were paid for (slug, name, price), cartId, status, paidAt, and payment details.**

**Approved Response Structure** (all payment types):
```json
{
  "cartId": "uuid",
  "status": "APPROVED",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "Cartão de Crédito|Cartão de Débito|PIX",
    "lastDigits": "1234",  // Card only
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    { "slug": "basic", "name": "Basic", "price": 19.90 },
    { "slug": "premium", "name": "Premium", "price": 39.90 }
  ]
}
```

**Pix Approved Response**:
```json
{
  "cartId": "uuid",
  "status": "APPROVED",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "PIX",
    "pixCode": "00020126580014br.gov.bcb.pix..."
  },
  "products": [...]
}
```

**Declined Response** (402):
```json
{
  "displayType": "BLOCK",
  "errorCode": "PAYMENT_DECLINED",
  "cartId": "uuid",
  "frontendError": {
    "code": "P-1001",
    "title": "Pagamento não aprovado",
    "description": "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio.",
    "primaryButton": {
      "label": "Tentar novamente",
      "url": "RETRY"
    },
    "secondaryButton": {
      "label": "Trocar meio de pagamento",
      "url": "/checkout/pagamento"
    }
  }
}
```

---

### FR-SYNC-007: Cart Status Update and Event Publishing
**Priority**: Required  
**Status**: Confirmed

On successful payment:
- Update cart status to `PAID`
- Set `paid_at` timestamp
- Publish `ORDER_CREATED` and `PAYMENT_PROCESSED` events via outbox
- Invalidate cart cache

On declined payment:
- Cart remains `PENDING`
- Record payment attempt
- Publish `PAYMENT_PROCESSED` (DECLINED) event via outbox

---

## 3. Business Rules

### BR-SYNC-001: Cart Must Be Pending
Only carts with status `PENDING` can be checked out. Carts in `PAID`, `EXPIRED`, `CANCELLED`, or `ABANDONED` states are rejected.

### BR-SYNC-002: Payment Method Must Match Offer
The payment method type must be one of the methods returned in pre-checkout for that cart. This ensures the frontend only offers methods that were valid at cart creation time.

### BR-SYNC-003: No Duplicate Approval
A cart with status `PAID` cannot be checked out again. This is enforced at both application and database level.

### BR-SYNC-004: Card Declination Rule
Card numbers ending in `0002` are always declined, regardless of other validation.

### BR-SYNC-005: Sensitive Data Not Returned
Full card numbers, CVV, expiry dates, and Pix keys are never returned in responses. Only masked/tokenized data is exposed.

### BR-SYNC-006: Idempotency Key Recommended
Clients should provide an `Idempotency-Key` header to protect against duplicate requests due to network retries.

### BR-SYNC-007: Cart Expiration
Carts expire after 30 minutes (configurable). Expired carts cannot be checked out.

### BR-SYNC-008: Products Returned on Success
The successful checkout response must include the products that were paid for (slug, name, price), matching the cart's product snapshots from pre-checkout.

---

## 4. User Stories

### US-SYNC-001: Successful Card Payment
**As a** frontend application  
**I want to** submit a cart with credit card payment data  
**So that** the user's subscription is activated immediately and they receive confirmation with the products they purchased

**Acceptance Criteria**: See AC-SYNC-001

---

### US-SYNC-002: Handle Declined Payment
**As a** frontend application  
**I want to** receive a clear error when payment is declined  
**So that** the user can choose another payment method

**Acceptance Criteria**: See AC-SYNC-002

---

### US-SYNC-003: Process Pix Payment
**As a** frontend application  
**I want to** submit a cart with Pix payment method  
**So that** the user receives a Pix code to complete payment

**Acceptance Criteria**: See AC-SYNC-003

---

### US-SYNC-004: Prevent Duplicate Payment
**As a** frontend application  
**I want to** be protected from duplicate charges if the user double-clicks or retries  
**So that** the user is not charged twice

**Acceptance Criteria**: See AC-SYNC-004

---

## 5. Acceptance Criteria

### AC-SYNC-001: Successful Sync Checkout (Card)
**Given** a valid cart with credit card payment method  
**And** card does not end in 0002  
**And** valid card data (number, holder, expiry, CVV)  
**When** POST /api/v1/operations/checkout  
**Then** 201 Created with:
- `status`: "APPROVED"
- `cartId`: UUID
- `paidAt`: ISO 8601 timestamp
- `products`: array of paid products with slug, name, price
- `payment.method`: "Cartão de Crédito" or "Cartão de Débito"
- `payment.lastDigits`: last 4 digits only
- `payment.amount`: matches cart total
- `payment.currency`: "BRL"
- Cart status updated to PAID in database
- ORDER_CREATED and PAYMENT_PROCESSED events published

---

### AC-SYNC-002: Declined Payment (Card 0002)
**Given** a valid cart with card ending in 0002  
**When** POST /api/v1/operations/checkout  
**Then** 402 Payment Declined with:
- `displayType`: "BLOCK"
- `errorCode`: "PAYMENT_DECLINED"
- `cartId`: UUID
- `frontendError.code`: "P-1001"
- `frontendError.title`: "Pagamento não aprovado"
- `frontendError.description`: "Não foi possível concluir o pagamento. Tente novamente ou escolha outro meio."
- `frontendError.primaryButton.action`: "RETRY"
- `frontendError.secondaryButton.action`: "/checkout/pagamento"
- Cart status remains PENDING
- PAYMENT_PROCESSED (DECLINED) event published

---

### AC-SYNC-003: Successful Pix Payment
**Given** a valid cart with Pix payment method  
**And** valid PIX payer data (name and valid CPF with check digits)  
**When** POST /api/v1/operations/checkout  
**Then** 201 Created with:
- `status`: "APPROVED"
- `cartId`: UUID
- `paidAt`: ISO 8601 timestamp
- `products`: array of paid products with slug, name, price
- `payment.method`: "PIX"
- `payment.pixCode`: valid BR Code format string
- Cart status updated to PAID

---

### AC-SYNC-004: Idempotency Protection
**Given** a valid cart  
**And** first request with Idempotency-Key: "abc-123" succeeds  
**When** second request with same Idempotency-Key: "abc-123" is sent  
**Then** 409 Conflict with:
- `errorCode`: "ALREADY_PAID" or "IDEMPOTENCY_CONFLICT"
- No duplicate payment processing

---

### AC-SYNC-005: Invalid Cart (Not Found)
**Given** a non-existent cart ID  
**When** POST /api/v1/operations/checkout  
**Then** 404 Not Found with:
- `displayType`: "BLOCK"
- `errorCode`: "INVALID_CART"
- `cartId`: UUID
- `frontendError.code`: "P-1000"
- `frontendError.title`: "Carrinho inválido"
- `frontendError.description`: "Carrinho não encontrado."
- `frontendError.primaryButton.action`: "RETRY"

---

### AC-SYNC-006: Already Paid Cart
**Given** a cart with status PAID  
**When** POST /api/v1/operations/checkout  
**Then** 409 Conflict with:
- `displayType`: "BLOCK"
- `errorCode`: "ALREADY_PAID"
- `cartId`: UUID
- `frontendError.code`: "P-1002"
- `frontendError.title`: "Carrinho já pago"
- `frontendError.description`: "Não foi possível concluir o pagamento: carrinho já foi previamente pago."
- `frontendError.primaryButton.action`: "GO_BACK" (url: "/")

---

### AC-SYNC-007: Expired Cart
**Given** a cart with status EXPIRED (TTL exceeded)  
**When** POST /api/v1/operations/checkout  
**Then** 404/410 with:
- `displayType`: "BLOCK"
- `errorCode`: "INVALID_CART"
- `frontendError.code`: "P-1000"

---

### AC-SYNC-008: Invalid Payment Method
**Given** a valid cart  
**And** a payment method type not offered during pre-checkout  
**When** POST /api/v1/operations/checkout  
**Then** 400/422 with:
- `displayType`: "BLOCK"
- `errorCode`: "INVALID_PAYMENT_DETAILS"
- `frontendError.code`: "P-1001"
- `frontendError.title`: "Método de pagamento inválido"
- `frontendError.description`: "Erro validando o método de pagamento, tente novamente com outro método."
- `frontendError.primaryButton.action`: "RETRY"

---

### AC-SYNC-009: Invalid Card Data (Luhn Failure)
**Given** a valid cart with card payment method  
**And** card number fails Luhn check  
**When** POST /api/v1/operations/checkout  
**Then** 402 Payment Declined (same as AC-SYNC-002)

---

### AC-SYNC-010: Invalid PIX Payer CPF
**Given** a valid cart with Pix payment method  
**And** invalid PIX payer CPF (fails check digit validation)  
**When** POST /api/v1/operations/checkout  
**Then** 402 Payment Declined (same as AC-SYNC-002)

---

### AC-SYNC-011: Missing Idempotency Key (Warning)
**Given** a valid checkout request without Idempotency-Key header  
**When** POST /api/v1/operations/checkout  
**Then** Request is processed (no blocking)  
**But** system logs warning for observability

---

### AC-SYNC-012: Cart with No Items
**Given** a cart that exists but has no items  
**When** POST /api/v1/operations/checkout  
**Then** 400 Bad Request with:
- `errorCode`: "INVALID_REQUEST"
- `description`: "Cart has no items"

---

## 6. Error Scenarios

| Scenario | HTTP Status | Error Code | Frontend Code | Description |
|----------|-------------|------------|---------------|-------------|
| Cart not found | 404 | INVALID_CART | P-1000 | Cart not found |
| Cart already paid | 409 | ALREADY_PAID | P-1002 | Cart already paid |
| Cart has no items | 400 | INVALID_REQUEST | - | Cart has no items |
| Invalid payment method | 400/422 | INVALID_PAYMENT_DETAILS | P-1001 | Invalid payment method |
| Payment declined (card 0002) | 402 | PAYMENT_DECLINED | P-1001 | Payment declined |
| Payment declined (invalid Luhn) | 402 | PAYMENT_DECLINED | P-1001 | Payment declined |
| Payment declined (invalid Pix) | 402 | PAYMENT_DECLINED | P-1001 | Payment declined |
| Idempotency conflict | 409 | IDEMPOTENCY_CONFLICT | - | Duplicate request |
| Service unavailable | 503 | SERVICE_UNAVAILABLE | P-9999 | Service temporarily unavailable |
| Internal error | 500 | INTERNAL_ERROR | P-9998 | Unexpected error |

---

## 7. Traceability

| Requirement | User Story | Acceptance Criteria | Business Rule |
|-------------|------------|---------------------|---------------|
| FR-SYNC-001 | US-SYNC-001 | AC-SYNC-001 | BR-SYNC-001..008 |
| FR-SYNC-002 | US-SYNC-001 | AC-SYNC-005, AC-SYNC-006, AC-SYNC-007, AC-SYNC-012 | BR-SYNC-001, BR-SYNC-007 |
| FR-SYNC-003 | US-SYNC-001 | AC-SYNC-008 | BR-SYNC-002 |
| FR-SYNC-004 | US-SYNC-004 | AC-SYNC-004, AC-SYNC-011 | BR-SYNC-003, BR-SYNC-006 |
| FR-SYNC-005 | US-SYNC-001..003 | AC-SYNC-001..003, AC-SYNC-009..010 | BR-SYNC-004 |
| FR-SYNC-006 | US-SYNC-001..003 | AC-SYNC-001..003 | BR-SYNC-005, BR-SYNC-008 |
| FR-SYNC-007 | US-SYNC-001..003 | AC-SYNC-001..003 | — |

---

## 8. Assumptions and Constraints

### Assumptions
- Pre-checkout has been completed successfully
- Cart exists and is in PENDING state
- Payment Methods Service is available (may be cached)
- Idempotency key is generated by frontend (UUID v4)
- Payment simulation rules are fixed (no real gateway)

### Constraints
- No real payment gateway integration
- Card numbers validated via Luhn algorithm
- Pix key format validation per BCB specification
- Full PAN never logged or persisted
- Maximum 30 minute cart TTL

---

## 9. Open Questions

None at this time.