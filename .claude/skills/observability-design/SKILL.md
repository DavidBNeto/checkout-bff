---
name: observability-design
description: Specify logs, metrics, traces, correlation and operational signals for backend services. Use when designing production readiness, async workflows or troubleshooting behavior.
---

# Observability Design

## Logs
Define structured fields such as timestamp, severity, service, environment, trace ID, span ID, correlation ID and operation name. Log state transitions and failures with useful context, but never log credentials, tokens, payment secrets or unnecessary personal data.

## Metrics
Choose metrics tied to service behavior:
- Request rate, error rate and latency.
- Dependency latency and failures.
- Queue depth/lag and message age.
- Retry and dead-letter counts.
- Processing duration and outcome counts.
Avoid unbounded-cardinality labels such as raw user, order or correlation IDs.

## Tracing
Propagate trace/correlation context across HTTP and messaging boundaries. Explain how asynchronous producer/consumer spans are linked.

## Alerting
For each alert document symptom, threshold rationale, severity, owner and runbook action. Do not invent thresholds without workload/SLO evidence; mark them as proposals.

## Deliverables
Produce an observability specification, dashboard/alert proposal and runbook notes for critical flows.
