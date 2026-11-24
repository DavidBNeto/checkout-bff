---
name: distributed-systems-resilience
description: Design failure handling and resilience for distributed backend services. Use when defining retries, timeouts, idempotency, concurrency, consistency or dependency failure behavior.
---

# Distributed Systems Resilience

## Required analysis
For each remote call or asynchronous boundary, document:
- Timeout and cancellation behavior.
- Retry eligibility, limits and backoff.
- Idempotency key scope, storage and retention.
- Duplicate and concurrent request behavior.
- Circuit breaking or load shedding where justified.
- Partial failure and compensation behavior.
- Consistency model and source of truth.
- Recovery, replay and operator intervention.

## Checkout-specific considerations
For payment/order workflows, explicitly distinguish:
- Request accepted from business operation completed.
- Payment authorization from capture/settlement if applicable.
- Payment result from order status.
- Client retry from internal message redelivery.
- Unknown outcome from confirmed failure.

Do not invent payment lifecycle semantics. Confirm them with business requirements and integration contracts.

## Anti-patterns
- Blind retries of non-idempotent operations.
- Treating a timeout as proof that a remote operation failed.
- Claiming exactly-once processing without end-to-end evidence.
- Using cache as the authoritative record without an explicit decision.
- Swallowing errors or acknowledging messages before durable processing.

## Output
Create a failure-mode table with trigger, observed behavior, recovery, owner and test scenario.
