# Payment Functional Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Business Analyst

---

## 1. Overview

This document defines the payment processing rules, simulation logic, and business rules for the Checkout BFF. Payment processing is simulated - no real payment gateway integration.

---

## 2. Payment Methods

### 2.1 Supported Methods

| Method | Type | Description |
|--------|------|-------------|
| Cartão de Crédito | CREDIT_CARD | Credit card payment |
| Cartão de Débito | DEBIT_CARD | Debit card payment |
| Pix | PIX | Brazilian instant payment system |

### 2.2 Payment Method Properties (from Pre-Checkout)

```json
{
  "name": "Cartão de Crédito"
}
```

The pre-checkout response returns an array of payment method names:
- "Cartão de Crédito"
- "Cartão de Débito"
- "Pix"

---

## 3. Payment Simulation Rules

### 3.1 Card Payment Rules (Credit & Debit)

| Card Pattern | Result | Details |
|--------------|--------|---------|
| Ends in `0002` | DECLINED | Simulates insufficient funds |
| Ends in `0001` | APPROVED | Simulates successful payment |
| Other valid Luhn | APPROVED | Generic approval |
| Invalid Luhn | DECLINED | Invalid card number |

### 3.2 Pix Payment Rules

| Condition | Result | Details |
|-----------|--------|---------|
| Valid PIX payer (name + valid CPF) | APPROVED | Returns simulated Pix copy-paste code (BR Code format) |
| Invalid PIX payer (missing name or invalid CPF) | DECLINED | Invalid payer data |

---

## 4. Payment Data Requirements

### 4.1 Card Payment Data (CREDIT_CARD / DEBIT_CARD)

```json
{
  "type": "CREDIT_CARD",
  "card": {
    "holderName": "JOHN DOE",
    "number": "4111111111111111",
    "expiry": "12/28",
    "cvv": "123"
  }
}
```

**Field Constraints**:
- `holderName`: Required, min 1 char
- `number`: Required, 15-20 digits
- `expiry`: Required, pattern MM/YY
- `cvv`: Required, 3-4 digits

### 4.2 Pix Payment Data (PIX)

```json
{
  "type": "PIX",
  "pixPayer": {
    "name": "JOHN DOE",
    "cpf": "52998224725"
  }
}
```

**Field Constraints**:
- `name`: Required, min 1 char
- `cpf`: Required, 11 digits, validated with check digits (Stella library)

---

## 5. Response Formats

### 5.1 Approved Card Response (Credit/Debit)

```json
{
  "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "APPROVED",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "Cartão de Crédito",
    "lastDigits": "1111",
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    { "slug": "premium", "name": "Premium", "price": 39.90 }
  ]
}
```

### 5.2 Approved Pix Response

```json
{
  "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "APPROVED",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "PIX",
    "pixCode": "00020126580014br.gov.bcb.pix0136a1b2c3d4-e5f6-7890-abcd-ef12345678905204000053039865802BR5913JOHN DOE6009SAO PAULO62070503***6304ABCD"
  },
  "products": [
    { "slug": "basic", "name": "Basic", "price": 19.90 }
  ]
}
```

### 5.3 Declined Response (402)

```json
{
  "displayType": "BLOCK",
  "errorCode": "PAYMENT_DECLINED",
  "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
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

## 6. Security Rules

### 6.1 Data Never Persisted
- Full card number (PAN)
- CVV/CVC
- Expiry date (stored separately if needed)
- PIX payer CPF (only for validation, not persisted)

### 6.2 Data Returned to Frontend
- Last 4 digits of card (approved only)
- Authorization code (approved only)
- Pix copy-paste code (approved only)

### 6.3 Data in Events/Logs
- Only masked/tokenized data
- Correlation IDs for tracing
- No sensitive payment data

---

## 7. Idempotency

### 7.1 Sync Checkout
- Idempotency key in header (`Idempotency-Key`)
- Prevents duplicate approval for same cart

### 7.2 Async Checkout
- Checkout ID is the idempotency key
- Payment worker checks processed events store
- Duplicate messages ACKed without reprocessing

---

## 8. Traceability

| Payment Rule | Functional Requirement | Acceptance Criteria |
|--------------|------------------------|---------------------|
| Card 0002 declined | FR-CHK-006 | AC-CHK-002 |
| Valid card approved | FR-CHK-006 | AC-CHK-001 |
| Pix approved with code | FR-CHK-006 | AC-CHK-003 |
| No sensitive data stored | NFR: Security | Scope §6 |
| Idempotency enforced | FR-CHK-005 | AC-CHK-005 |