# Documentation Index

**Project**: Checkout BFF - Subscription Checkout Backend for Frontend  
**Version**: 1.0  
**Last Updated**: 2026-11-23  

---

## 1. Product Documentation

| Document | Description | Status |
|----------|-------------|--------|
| [Product Vision](product/vision.md) | High-level product vision and success criteria | ✅ Confirmed |
| [Project Scope](product/scope.md) | Functional boundaries, in/out of scope, priorities | ✅ Confirmed |
| [Glossary](product/glossary.md) | Terminology definitions | ✅ Confirmed |
| [Roadmap](plans/roadmap.md) | Implementation roadmap and milestones | 📝 Draft |

---

## 2. Functional Specifications

| Document | Description | Status |
|----------|-------------|--------|
| [Pre-Checkout](specs/functional/pre-checkout.md) | Pre-checkout operation requirements, rules, acceptance criteria | ✅ Confirmed |
| [Checkout](specs/functional/checkout.md) | Synchronous & asynchronous checkout requirements | ✅ Confirmed |
| [Sync Checkout](specs/functional/sync-checkout.md) | Detailed synchronous checkout functional specification | ✅ Confirmed |
| [Payment](specs/functional/payment.md) | Payment simulation rules, methods, security | ✅ Confirmed |
| [Error Handling](specs/functional/error-handling.md) | Standardized error contract, codes, examples | ✅ Confirmed |

---

## 3. Technical Specifications

| Document | Description | Status |
|----------|-------------|--------|
| [API Contracts](specs/technical/api-contracts.md) | OpenAPI 3.1 contracts for all endpoints | ✅ Confirmed |
| [Architecture](specs/technical/architecture.md) | Clean Architecture layers, component diagrams | ✅ Confirmed |
| [Database](specs/technical/database.md) | Schema, ERD, migrations, JPA mappings | ✅ Confirmed |
| [Caching](specs/technical/caching.md) | Redis caching strategy, keys, TTL, invalidation | ✅ Confirmed |
| [Messaging](specs/technical/messaging.md) | RabbitMQ topology, events, outbox, consumers | ✅ Confirmed |
| [Observability](specs/technical/observability.md) | Logging, metrics, tracing, health checks, alerting | ✅ Confirmed |
| [Security](specs/technical/security.md) | Data protection, API security, compliance | ✅ Confirmed |

---

## 4. Diagrams (Mermaid)

| Diagram | Description | Location |
|---------|-------------|----------|
| Architecture Overview | System context and layers | [architecture.mmd](specs/diagrams/architecture.mmd) |
| Checkout Sequence | Pre-checkout + sync checkout flow | [checkout-sequence.mmd](specs/diagrams/checkout-sequence.mmd) |
| Async Payment Sequence | Async checkout + worker flow | [async-payment-seq.mmd](specs/diagrams/async-payment-seq.mmd) |
| Database ERD | Entity relationship diagram | [database-erd.mmd](specs/diagrams/database-erd.mmd) |
| Payment State Machine | Cart/checkout status transitions | [payment-state.mmd](specs/diagrams/payment-state.mmd) |
| Use Cases | Functional use case diagram | [use-cases.mmd](specs/diagrams/use-cases.mmd) |

---

## 5. Traceability Matrix

### 5.1 Functional → Technical

| Functional Spec | Technical Specs |
|-----------------|-----------------|
| Pre-Checkout | API Contracts (API-PRE-*), Architecture, Database, Caching, Error Handling |
| Checkout | API Contracts (API-CHK-*), Architecture, Database, Messaging, Security |
| Payment | API Contracts, Architecture, Messaging, Security |
| Error Handling | API Contracts, Architecture, Observability |

### 5.2 Requirements Coverage

| ID | Description | Functional Spec | Technical Spec | Test Cases |
|----|-------------|-----------------|----------------|------------|
| FR-PRE-001 | Create cart from slugs | pre-checkout.md | api-contracts.md | TC-PRE-001 |
| FR-PRE-002 | Retrieve products | pre-checkout.md | architecture.md, caching.md | TC-PRE-002 |
| FR-PRE-003 | Retrieve payment methods | pre-checkout.md | architecture.md, caching.md | TC-PRE-003 |
| FR-PRE-004 | Return standardized response | pre-checkout.md | api-contracts.md | TC-PRE-001 |
| FR-PRE-005 | Reject invalid products | pre-checkout.md, error-handling.md | api-contracts.md | TC-PRE-002, TC-PRE-003 |
| FR-CHK-001 | Sync checkout | checkout.md | api-contracts.md, messaging.md | TC-CHK-001 |
| FR-CHK-002 | Async checkout | checkout.md | api-contracts.md, messaging.md | TC-CHK-007 |
| FR-CHK-005 | Duplicate prevention | checkout.md, payment.md | database.md, messaging.md | TC-CHK-005 |
| NFR-SEC-001 | No card storage | payment.md, security.md | database.md | TC-SEC-001 |
| NFR-OBS-001 | Correlation IDs | error-handling.md, observability.md | messaging.md | TC-OBS-001 |

---

## 6. Document Conventions

### 6.1 Status Values
- ✅ **Confirmed** - Approved and baselined
- 📝 **Draft** - Under review, not yet approved
- ❓ **Proposed** - Suggested, needs discussion
- 🔄 **In Progress** - Being actively worked on

### 6.2 Requirement IDs
| Prefix | Type | Example |
|--------|------|---------|
| FR-* | Functional Requirement | FR-PRE-001 |
| NFR-* | Non-Functional Requirement | NFR-SEC-001 |
| BR-* | Business Rule | BR-PRE-001 |
| API-* | API Contract | API-PRE-001 |
| AC-* | Acceptance Criteria | AC-PRE-001 |
| US-* | User Story | US-PRE-001 |
| TC-* | Test Case | TC-PRE-001 |
| ADR-* | Architecture Decision | ADR-001 |

### 6.3 Cross-References
Use relative links between documents:
```markdown
[Pre-Checkout Specification](specs/functional/pre-checkout.md)
[API Contracts](specs/technical/api-contracts.md#2-pre-checkout-api)
```

---

## 7. Maintenance

### 7.1 Review Cycle
- **Major releases**: Full documentation review
- **Minor changes**: Update affected documents only
- **Quarterly**: Traceability matrix validation

### 7.2 Change Process
1. Identify affected documents
2. Update with change tracking
3. Validate cross-references
4. Update traceability matrix
5. Review with stakeholders

---

## 8. Quick Links

### 8.1 For Developers
- [Architecture Overview](specs/technical/architecture.md)
- [API Contracts](specs/technical/api-contracts.md)
- [Database Schema](specs/technical/database.md)
- [Error Handling](specs/functional/error-handling.md)

### 8.2 For QA
- [Pre-Checkout Acceptance Criteria](specs/functional/pre-checkout.md#5-acceptance-criteria)
- [Checkout Acceptance Criteria](specs/functional/checkout.md#4-acceptance-criteria)
- [Sync Checkout Acceptance Criteria](specs/functional/sync-checkout.md#5-acceptance-criteria)
- [Payment Rules](specs/functional/payment.md#3-payment-simulation-rules)
- [Error Codes](specs/functional/error-handling.md#3-error-codes-catalog)

### 8.3 For Architects
- [Architecture Decisions](decisions/) (ADRs)
- [System Context](specs/technical/architecture.md#3-pre-checkout-component-diagram)
- [Data Flow](specs/technical/architecture.md#5-data-flow-architecture)
- [Security Model](specs/technical/security.md)

### 8.4 For DevOps
- [Observability](specs/technical/observability.md)
- [Health Checks](specs/technical/observability.md#5-health-checks)
- [Alerting Rules](specs/technical/observability.md#6-alerting-rules)
- [Docker Compose](../docker-compose.yml)