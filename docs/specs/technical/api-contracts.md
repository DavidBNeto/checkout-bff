# API Contracts Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Technical Analyst

---

## 1. Overview

This document defines the API contracts for the Checkout BFF, including the Pre-Checkout endpoint and related operations. All contracts follow the OpenAPI 3.1 specification.

---

## 2. Pre-Checkout

### 2.1 Endpoint Definition

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/operations/pre-checkout` |
| **Content-Type** | `application/json` |
| **Accept** | `application/json` |

### 2.2 Request

#### Request Body Schema

```json
{
  "type": "object",
  "required": ["products"],
  "properties": {
    "products": {
      "type": "array",
      "minItems": 1,
      "maxItems": 50,
      "items": {
        "type": "object",
        "required": ["slug"],
        "properties": {
          "slug": {
            "type": "string",
            "pattern": "^[a-z0-9-]+$",
            "minLength": 1,
            "maxLength": 100,
            "description": "Product slug identifier (e.g., 'basic', 'premium', 'family')"
          }
        },
        "additionalProperties": false
      },
      "description": "List of products selected by the user for checkout",
      "uniqueItems": true
    }
  },
  "example": {
    "products": [
      { "slug": "basic" },
      { "slug": "premium" }
    ]
  }
}
```

#### Field Constraints

| Field | Required | Type | Constraints |
|-------|----------|------|-------------|
| `products` | Yes | `object[]` | 1-50 items, each with `slug`, unique slugs |
| `products[].slug` | Yes | `string` | Alphanumeric + hyphens, 1-100 chars |

### 2.3 Success Response

#### HTTP Status: `201 Created`

#### Response Body Schema

```json
{
  "type": "object",
  "required": ["cartId", "products", "paymentMethods"],
  "properties": {
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Unique identifier for the created checkout cart"
    },
    "products": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": ["slug", "name", "description", "price", "currency"],
        "properties": {
          "slug": {
            "type": "string",
            "description": "Product slug identifier"
          },
          "name": {
            "type": "string",
            "description": "Product display name"
          },
          "description": {
            "type": "string",
            "description": "Product description"
          },
          "price": {
            "type": "number",
            "format": "decimal",
            "minimum": 0,
            "description": "Product price in the specified currency"
          },
          "currency": {
            "type": "string",
            "pattern": "^[A-Z]{3}$",
            "description": "ISO 4217 currency code"
          }
        },
        "additionalProperties": false
      },
      "description": "Product details for each requested slug (no internal IDs)"
    },
    "paymentMethods": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": [ "name" ],
        "properties": {
          "name": {
            "type": "string",
            "description": "Payment Method display name"
          },
        }
        "additionalProperties": false
      },
      "description": "Available payment methods for the submitted cart"
    }
  }
}
```

#### Success Response Example

```json
{
  "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "products": [
    {
      "slug": "basic",
      "name": "Basic",
      "description": "Basic subscription plan",
      "price": 19.90,
      "currency": "BRL"
    },
    {
      "slug": "premium",
      "name": "Premium",
      "description": "Premium subscription plan",
      "price": 39.90,
      "currency": "BRL"
    }
  ],
  "paymentMethods": [
    { "name": "Cartão de Crédito" },
    { "name": "Cartão de Débito" },
    { "name": "Pix" }
  ]
}
```

**Note**: The response intentionally omits:
- Internal product IDs (`id` field)
- Any internal identifiers that could leak implementation details

### 2.4 Error Responses

#### Error Response Schema

```json
{
  "type": "object",
  "required": [ "displayType", "errorCode", "frontendError" ],
  "properties": {
    "displayType": {
      "type": "string",
      "const": "BLOCK",
      "description": "Defines how the frontend should display the error"
    },
    "errorCode": {
      "type": "string",
      "minLength": 1,
      "description": "Unique identifier for the checkout error"
    },
    "frontendError": {
      "type": "object",
      "required": [ "code", "title", "description", "primaryButton" ],
      "properties": {
        "code": {
          "type": "string",
          "minLength": 1,
          "description": "Frontend-specific error identifier"
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Error title displayed to the customer"
        },
        "description": {
          "type": "string",
          "minLength": 1,
          "description": "Detailed error message displayed to the customer"
        },
        "primaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Primary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "example": {
    "displayType": "BLOCK",
    "errorCode": "INVALID_REQUEST",
    "frontendError": {
      "code": "P-1007",
      "title": "Carrinho Inválido",
      "description": "Pelo menos um produto é necessário.",
      "primaryButton": {
        "label": "Tentar novamente",
        "url": "RETRY"
      }
    }
  }
}
```

#### 400 Bad Request - Invalid Request

```json
{
    "displayType": "BLOCK",
    "errorCode": "INVALID_REQUEST",
    "frontendError": {
        "code": "P-1007",
        "title": "Carrinho Inválido",
        "description": "Pelo menos um produto é necessário.",
        "primaryButton": {
          "label": "Tentar novamente",
          "url": "RETRY"
        }
    }
}
```

#### 400/422 Bad Request - Product Not Found

```json
{
    "displayType": "BLOCK",
    "errorCode": "PRODUCT_NOT_FOUND",
    "frontendError": {
        "code": "P-1004",
        "title": "Produto não encontrado",
        "description": "Os seguintes produtos não foram encontrados: slug-inválida, slug-inválida2",
        "primaryButton": {
            "label": "Retornar",
            "url": "/"
        }
    }
}
```

#### 400/422 Bad Request - Product Unavailable

```json
{
    "displayType": "BLOCK",
    "errorCode": "PRODUCT_UNAVAILABLE",
    "frontendError": {
        "code": "P-1005",
        "title": "Produto indisponível",
        "description": "Os seguintes não estão disponíveis: slug-inválida1, slug-inválida2",
        "primaryButton": {
            "label": "Retornar",
            "url": "/"
        }
    }
}
```

#### 503 Service Unavailable

```json
{
    "displayType": "BLOCK",
    "errorCode": "SERVICE_UNAVAILABLE",
    "frontendError": {
        "code": "P-9999",
        "title": "Serviço de pagamento indisponível",
        "description": "Não foi possível concluir o pagamento, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 500 Internal Server Error

```json
{
  {
    "displayType": "BLOCK",
    "errorCode": "INTERNAL_ERROR",
    "frontendError": {
        "code": "P-9998",
        "title": "Erro interno inesperado",
        "description": "Um errro inesperado aconteceu, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
  }
}
```

### 2.5 Headers

#### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Content-Type` | Yes | Must be `application/json` |
| `Accept` | Yes | Must be `application/json` |
| `X-Correlation-ID` | No | Client-generated correlation ID for tracing |
| `Idempotency-Key` | No | Optional idempotency key for duplicate protection |

#### Response Headers

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |
| `X-Correlation-ID` | Echoed from request or generated |
| `Location` | URI of created cart resource (optional) |

---

## 3. Synchronous Checkout

### 3.1 Endpoint Definition

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/operations/checkout` |
| **Content-Type** | `application/json` |
| **Accept** | `application/json` |

### 3.2 Request

#### Request Body Schema

```json

{
  "type": "object",
  "required": ["cartId", "paymentMethod"],
  "properties": {
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the cart being checked out"
    },
    "paymentMethod": {
      "oneOf": [
        {
          "type": "object",
          "required": ["type", "card"],
          "properties": {
            "type": {
              "type": "string",
              "const": "CREDIT_CARD",
              "description": "Credit card payment method"
            },
            "card": {
              "type": "object",
              "required": [ "holderName", "number", "expiry", "cvv" ],
              "properties": {
                "holderName": {
                  "type": "string",
                  "minLength": 1,
                  "description": "Cardholder name"
                },
                "number": {
                  "type": "string",
                  "minLength": 15,
                  "maxLength": 20,
                  "description": "Credit card number"
                },
                "expiry": {
                  "type": "string",
                  "pattern": "^(0[1-9]|1[0-2])/[0-9]{2}$",
                  "description": "Card expiration date (MM/YY)"
                },
                "cvv": {
                  "type": "string",
                  "minLength": 3,
                  "maxLength": 4,
                  "description": "Credit card security code"
                }
              },
              "additionalProperties": false
            }
          },
          "additionalProperties": false
        },
        {
          "type": "object",
          "required": ["type", "card"],
          "properties": {
            "type": {
              "type": "string",
              "const": "DEBIT_CARD",
              "description": "Debit card payment method"
            },
            "card":  {
              "type": "object",
              "required": [ "holderName", "number", "expiry", "cvv" ],
              "properties": {
                "holderName": {
                  "type": "string",
                  "minLength": 1,
                  "description": "Cardholder name"
                },
                "number": {
                  "type": "string",
                  "minLength": 15,
                  "maxLength": 20,
                  "description": "Credit card number"
                },
                "expiry": {
                  "type": "string",
                  "pattern": "^(0[1-9]|1[0-2])/[0-9]{2}$",
                  "description": "Card expiration date (MM/YY)"
                },
                "cvv": {
                  "type": "string",
                  "minLength": 3,
                  "maxLength": 4,
                  "description": "Credit card security code"
                }
              },
              "additionalProperties": false
            }
          },
          "additionalProperties": false
        },
        {
          "type": "object",
          "required": ["type"],
          "properties": {
            "type": {
              "type": "string",
              "const": "PIX",
              "description": "PIX payment method"
            }
          },
          "additionalProperties": false
        }
      ],
      "description": "Payment method selected for checkout"
    }
  },
  "additionalProperties": false,
  "example": {
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
    "paymentMethod": {
      "type": "CREDIT_CARD",
      "card": {
        "holderName": "Ana Souza",
        "token": "card-token-reference",
        "expiry": "12/28",
        "installments": 3
      }
    }
  }
}

```

#### Field Constraints

| Field | Required | Type | Constraints |
|-------|----------|------|-------------|
| `cartId` | Yes | `string` | Is a valid UUID of an unpayed cart |
| `paymentMethod` | Yes | `object` | Is a valid payment method that was provided for that cartId |

### 3.3 Success Response

#### HTTP Status: `201 Created`

#### Response Body Schema

```json

{
  "type": "object",
  "required": [ "cartId", "status", "paidAt", "payment", "products" ],
  "properties": {
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the processed cart"
    },
    "status": {
      "type": "string",
      "enum": [ "APPROVED", "PENDING" ], 
      "description": "Payment approval status"
    },
    "paidAt": {
      "type": "string",
      "format": "date-time",
      "description": "Payment approval timestamp in ISO 8601 format"
    },
    "payment": {
      "oneOf": [
        {
          "type": "object",
          "required": [ "method", "lastDigits", "amount", "currency" ],
          "properties": {
            "method": {
              "type": "string",
              "enum": [ "Cartão de Crédito", "Cartão de Débito" ],
              "description": "Approved payment method"
            },
            "lastDigits": {
              "type": "string",
              "pattern": "^[0-9]{4}$",
              "description": "Last four digits of the payment card"
            },
            "amount": {
              "type": "number",
              "minimum": 0,
              "multipleOf": 0.01,
              "description": "Total amount paid"
            },
            "currency": {
              "type": "string",
              "const": "BRL",
              "description": "Payment currency"
            }
          },
          "additionalProperties": false
        },
        {
          "type": "object",
          "required": [ "method", "pixCode" ],
          "properties": {
            "method": {
              "type": "string",
              "const": "PIX",
              "description": "Approved payment method"
            },
            "pixCode": {
              "type": "string",
              "description": "Pix code used for the payment "
            },
          },
          "additionalProperties": false
        },
      ]
    },
    "products": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": [ "slug", "name", "price" ],
        "properties": {
          "slug": {
            "type": "string",
            "pattern": "^[a-z0-9-]+$",
            "minLength": 1,
            "maxLength": 100,
            "description": "Product slug identifier"
          },
          "name": {
            "type": "string",
            "minLength": 1,
            "description": "Product display name"
          },
          "price": {
            "type": "number",
            "minimum": 0,
            "multipleOf": 0.01,
            "description": "Individual product price"
          }
        },
        "additionalProperties": false
      },
      "description": "Products included in the approved checkout"
    }
  },
  "additionalProperties": false,
  "example": {
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "APPROVED",
    "paidAt": "2026-10-14T16:00:00Z",
    "payment": {
      "method": "Cartão de Crédito",
      "lastDigits": "4242",
      "amount": 39.90,
      "currency": "BRL"
    },
    "products": [
      {
        "slug": "premium",
        "name": "Plano Premium",
        "price": 39.90
      }
    ]
  }
}

```

#### Success Response Example

```json
{
  "cartId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "APPROVED",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "Cartão de Crédito",
    "lastDigits": "4242",
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    {
      "slug": "premium",
      "name": "Plano Premium",
      "price": 39.90
    }
  ]
}
```

### 3.4 Error Responses

#### Error response schema

```json
{
  "type": "object",
  "required": [ "displayType", "errorCode", "cartId", "frontendError" ],
  "properties": {
    "displayType": {
      "type": "string",
      "enum": ["BLOCK", "INTERVENTION"],
      "description": "Defines how the frontend should display the error"
    },
    "errorCode": {
      "type": "string",
      "minLength": 1,
      "description": "Unique identifier for the checkout error"
    },
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the cart associated with the failed checkout"
    },
    "frontendError": {
      "type": "object",
      "required": [ "code", "title", "description", "primaryButton" ],
      "properties": {
        "code": {
          "type": "string",
          "minLength": 1,
          "description": "Frontend-specific error identifier"
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Error title displayed to the customer"
        },
        "description": {
          "type": "string",
          "minLength": 1,
          "description": "Detailed error message displayed to the customer"
        },
        "primaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Primary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        },
        "secondaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Secondary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "example": {
    "displayType": "BLOCK",
    "errorCode": "PAYMENT_DECLINED",
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
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
}

```

#### 402 Payment Required - Payment Declined / Invalid Payment

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


#### 404 Not found - Cart not found

```json
{
    "displayType": "BLOCK",
    "errorCode": "INVALID_CART",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1000",
        "title": "Carrinho inválido",
        "description": "Carrinho não encontrado.",
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

#### 409 Conflict - Cart Already paid

```json
{
    "displayType": "BLOCK",
    "errorCode": "ALREADY_PAID",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1002",
        "title": "Carrinho já pago",
        "description": "Não foi possível concluir o pagamento: carrinho já foi previamente pago.",
        "primaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 503 Service Unavailable

```json
{
    "displayType": "BLOCK",
    "errorCode": "SERVICE_UNAVAILABLE",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Serviço de pagamento indisponível",
        "description": "Não foi possível concluir o pagamento, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 500 Internal Server Error

```json
{
  {
    "displayType": "BLOCK",
    "errorCode": "INTERNAL_ERROR",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Erro interno inesperado",
        "description": "Um errro inesperado aconteceu, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
  }
}
```

### 3.5 Headers

#### Request Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Content-Type` | Yes | Must be `application/json` |
| `Accept` | Yes | Must be `application/json` |
| `X-Correlation-ID` | No | Client-generated correlation ID for tracing |
| `Idempotency-Key` | No | Optional idempotency key for duplicate protection |

#### Response Headers

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |
| `X-Correlation-ID` | Echoed from request or generated |
| `Location` | URI of created cart resource (optional) |

---


## 4. Asynchronous Checkout 

### 4.1 Endpoint Definition

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Path** | `/api/v1/operations/checkout/async` |
| **Content-Type** | `application/json` |
| **Accept** | `application/json` |

### 4.2 Request

The asynchronous checkout endpoint uses the same schemas for the request as the synchronous checkout endpoint. More data can be found [here](#32-request)

### 4.3 Success Response

#### HTTP Status: `202 Accepted`

#### Response Body Schema

```json
{
  "type": "object",
  "required": [ "cartId", "status", "paidAt", "payment", "products" ],
  "properties": {
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the processed cart"
    },
    "status": {
      "type": "string",
      "const": "PENDING", 
      "description": "Payment approval status"
    },
    "paidAt": {
      "type": "string",
      "format": "date-time",
      "description": "Payment approval timestamp in ISO 8601 format"
    },
    "payment": {
      "oneOf": [
        {
          "type": "object",
          "required": [ "method", "lastDigits", "amount", "currency" ],
          "properties": {
            "method": {
              "type": "string",
              "enum": [ "Cartão de Crédito", "Cartão de Débito" ],
              "description": "Approved payment method"
            },
            "lastDigits": {
              "type": "string",
              "pattern": "^[0-9]{4}$",
              "description": "Last four digits of the payment card"
            },
            "amount": {
              "type": "number",
              "minimum": 0,
              "multipleOf": 0.01,
              "description": "Total amount paid"
            },
            "currency": {
              "type": "string",
              "const": "BRL",
              "description": "Payment currency"
            }
          },
          "additionalProperties": false
        },
        {
          "type": "object",
          "required": [ "method", "pixCode" ],
          "properties": {
            "method": {
              "type": "string",
              "const": "PIX",
              "description": "Approved payment method"
            },
            "pixCode": {
              "type": "string",
              "description": "Pix code used for the payment "
            },
          },
          "additionalProperties": false
        },
      ]
    },
    "products": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "object",
        "required": [ "slug", "name", "price" ],
        "properties": {
          "slug": {
            "type": "string",
            "pattern": "^[a-z0-9-]+$",
            "minLength": 1,
            "maxLength": 100,
            "description": "Product slug identifier"
          },
          "name": {
            "type": "string",
            "minLength": 1,
            "description": "Product display name"
          },
          "price": {
            "type": "number",
            "minimum": 0,
            "multipleOf": 0.01,
            "description": "Individual product price"
          }
        },
        "additionalProperties": false
      },
      "description": "Products included in the approved checkout"
    }
  },
  "additionalProperties": false,
  "example": {
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "APPROVED",
    "paidAt": "2026-10-14T16:00:00Z",
    "payment": {
      "method": "Cartão de Crédito",
      "lastDigits": "4242",
      "amount": 39.90,
      "currency": "BRL"
    },
    "products": [
      {
        "slug": "premium",
        "name": "Plano Premium",
        "price": 39.90
      }
    ]
  }
}

```

#### Success Response Example

```json
{
  "cartId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "paidAt": "2026-10-14T16:00:00Z",
  "payment": {
    "method": "Cartão de Crédito",
    "lastDigits": "4242",
    "amount": 39.90,
    "currency": "BRL"
  },
  "products": [
    {
      "slug": "premium",
      "name": "Plano Premium",
      "price": 39.90
    }
  ]
}
```

### 4.4 Error Responses

#### Error response schema

```json
{
  "type": "object",
  "required": [ "displayType", "errorCode", "cartId", "frontendError" ],
  "properties": {
    "displayType": {
      "type": "string",
      "enum": ["BLOCK", "INTERVENTION"],
      "description": "Defines how the frontend should display the error"
    },
    "errorCode": {
      "type": "string",
      "minLength": 1,
      "description": "Unique identifier for the checkout error"
    },
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the cart associated with the failed checkout"
    },
    "frontendError": {
      "type": "object",
      "required": [ "code", "title", "description", "primaryButton" ],
      "properties": {
        "code": {
          "type": "string",
          "minLength": 1,
          "description": "Frontend-specific error identifier"
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Error title displayed to the customer"
        },
        "description": {
          "type": "string",
          "minLength": 1,
          "description": "Detailed error message displayed to the customer"
        },
        "primaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Primary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        },
        "secondaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Secondary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "example": {
    "displayType": "BLOCK",
    "errorCode": "PAYMENT_DECLINED",
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
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
}

```

#### 402 Payment Required - Invalid Payment

```json
{
    "displayType": "BLOCK",
    "errorCode": "INVALID_PAYMENT_DETAILS",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1001",
        "title": "Método de pagamento inválido",
        "description": "Erro validando o método de pagamento, tente novamente com outro método.",
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



#### 404 Not found - Cart not found

```json
{
    "displayType": "BLOCK",
    "errorCode": "INVALID_CART",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1000",
        "title": "Carrinho inválido",
        "description": "Carrinho não encontrado.",
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

#### 409 Conflict - Cart Already paid

```json
{
    "displayType": "BLOCK",
    "errorCode": "ALREADY_PAID",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1002",
        "title": "Carrinho já pago",
        "description": "Não foi possível concluir o pagamento: carrinho já foi previamente pago.",
        "primaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 503 Service Unavailable

```json
{
    "displayType": "BLOCK",
    "errorCode": "SERVICE_UNAVAILABLE",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Serviço de pagamento indisponível",
        "description": "Não foi possível concluir o pagamento, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 500 Internal Server Error

```json
{
  {
    "displayType": "BLOCK",
    "errorCode": "INTERNAL_ERROR",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Erro interno inesperado",
        "description": "Um errro inesperado aconteceu, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
  }
}
```

## 5. Cart status verification

### 5.1 Endpoint Definition

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Path** | `/api/v1/operations/cart/${cartId}` |
| **Accept** | `application/json` |

### 5.2 Request
Empty body, `cartId` sent as a path parameter.

### 5.3 Success Response

#### Response body schema
```json
{
  "type": "object",
  "required": [ "cartId", "status", "paidAt", "payment", "products" ],
  "properties": {
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the processed cart"
    },
    "status": {
      "type": "string",
      "enum": [ "APPROVED", "PENDING", "PROCESSING", "FAILED", "DENIED" ], 
      "description": "Payment approval status"
    },
  },
  "additionalProperties": false,
  "example": {
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "APPROVED"
  }
}
```

### 5.4 Error Response

#### Error response schema

```json
{
  "type": "object",
  "required": [ "displayType", "errorCode", "cartId", "frontendError" ],
  "properties": {
    "displayType": {
      "type": "string",
      "enum": ["BLOCK", "INTERVENTION"],
      "description": "Defines how the frontend should display the error"
    },
    "errorCode": {
      "type": "string",
      "minLength": 1,
      "description": "Unique identifier for the checkout error"
    },
    "cartId": {
      "type": "string",
      "format": "uuid",
      "description": "Identifier of the cart associated with the failed checkout"
    },
    "frontendError": {
      "type": "object",
      "required": [ "code", "title", "description", "primaryButton" ],
      "properties": {
        "code": {
          "type": "string",
          "minLength": 1,
          "description": "Frontend-specific error identifier"
        },
        "title": {
          "type": "string",
          "minLength": 1,
          "description": "Error title displayed to the customer"
        },
        "description": {
          "type": "string",
          "minLength": 1,
          "description": "Detailed error message displayed to the customer"
        },
        "primaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Primary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        },
        "secondaryButton": {
          "type": "object",
          "required": [ "label", "url" ],
          "properties": {
            "label": {
              "type": "string",
              "minLength": 1,
              "description": "Secondary action button label"
            },
            "url": {
              "type": "string",
              "minLength": 1,
              "description": "Action identifier or navigation URL"
            }
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "example": {
    "displayType": "BLOCK",
    "errorCode": "PAYMENT_DECLINED",
    "cartId": "550e8400-e29b-41d4-a716-446655440000",
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
}

```

#### 402 Payment Required - Payment Declined

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

#### 404 Not found - Cart not found

```json
{
    "displayType": "BLOCK",
    "errorCode": "INVALID_CART",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-1000",
        "title": "Carrinho inválido",
        "description": "Carrinho não encontrado.",
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

#### 503 Service Unavailable

```json
{
    "displayType": "BLOCK",
    "errorCode": "SERVICE_UNAVAILABLE",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Serviço de pagamento indisponível",
        "description": "Não foi possível concluir o pagamento, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
}
```

#### 500 Internal Server Error

```json
{
  {
    "displayType": "BLOCK",
    "errorCode": "INTERNAL_ERROR",
    "cartId": "uuid",
    "frontendError": {
        "code": "P-9999",
        "title": "Erro interno inesperado",
        "description": "Um errro inesperado aconteceu, tente novamente mais tarde.",
        "primaryButton": {
              "label": "Tentar novamente",
              "url": "RETRY"
        },
        "secondaryButton": {
            "label": "Voltar",
            "url": "/"
        }
    }
  }
}
```

## 6. Shared Data Types

### 6.1 Frontend Error Contract

All error responses follow this standardized contract:

```json
{
  "displayType": "BLOCK|INTERVENTION",
  "errorCode": "string",
  "cartId": "uuid", //not required for pre-checkout
  "frotendError": {
    "code": "string",
    "title": "string",
    "description": "string",
    "primaryButton": {
      "label": "string",
      "url": "string"
    },
    "secondaryButton": { //optional
      "label": "string",
      "url": "string"
    }
  }
}
```

**Display Types** (per glossary):
- `BLOCK`: Error that interrupts flow, requires user action
- `INTERVENTION`: Warning that doesn't terminate flow

### 6.2 Product Object (Snapshot)

```json
{
  "slug": "string",
  "name": "string",
  "description": "string",
  "price": "decimal",
  "currency": "string (ISO 4217)"
}
```

**Note**: The `id` field (internal product identifier) is intentionally omitted from the API response to avoid leaking implementation details.

---

## 7. Versioning Strategy

- API version in URL path: `/api/v1/` or `/api/v1/operations/`
- Breaking changes require new version
- Additive changes (new optional fields) are non-breaking
- Error codes are stable within a version

---

## 8. OpenAPI Document

The complete OpenAPI 3.1 specification is available at:
- **Development**: `http://localhost:8080/v3/api-docs`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

---
