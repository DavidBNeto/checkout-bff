---
name: backend-observability-readiness
description: Check whether a Spring Boot backend is operationally observable and diagnosable. Use when reviewing production readiness, health endpoints, metrics, logs and tracing.
---

# Backend Observability Readiness

Inspect existing dependencies and conventions before recommending additions.

Check:
- Liveness/readiness semantics and dependency checks.
- Structured logs and consistent correlation identifiers.
- Metrics for HTTP, JVM, database, cache and messaging where present.
- Trace propagation across HTTP and asynchronous boundaries.
- Sensitive-data redaction.
- Graceful shutdown and in-flight request/message behavior.
- Dashboards, alerts and runbooks for critical paths.

Do not add an observability stack or vendor dependency without an explicit need and approval. Report missing instrumentation separately from missing operational ownership.
