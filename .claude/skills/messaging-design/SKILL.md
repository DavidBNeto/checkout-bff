---
name: messaging-design
description: Design asynchronous messaging contracts and processing flows. Use when a backend feature uses queues, topics, events, consumers, retries or dead-letter handling.
---

# Messaging Design

## Discover first
Inspect existing broker, client libraries, topology, deployment and operational conventions. If none are established, keep broker choice open and document options; do not assume Kafka or RabbitMQ.

## Specify
For each message/event define:
- Stable ID and name, producer and consumer.
- Business meaning and trigger.
- Schema, required/optional fields and versioning.
- Message key/correlation/causation identifiers where relevant.
- Delivery and ordering expectations.
- Acknowledgment/commit behavior.
- Retry policy, backoff and maximum attempts.
- Dead-letter or parking-lot behavior and replay ownership.
- Idempotency/deduplication strategy.
- Timeout, poison-message and schema-compatibility behavior.
- Security, sensitive-data classification and observability.

## Distributed behavior
Assume at-least-once delivery unless the selected technology and design prove otherwise. Design consumers to tolerate duplicates. Do not promise exactly-once business effects based solely on broker guarantees.

Consider transactional outbox/inbox when database changes and message publication must remain consistent. Explain the trade-offs and operational burden.

## Deliverables
Produce an event/message catalog, schema examples, sequence diagram, failure matrix and unresolved decisions. Validate schemas and examples where tooling exists.
