# Observability Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Solution Architect / DevOps Engineer

---

## 1. Overview

This document defines the observability requirements for the Checkout BFF, including logging, metrics, tracing, and health checks.

---

## 2. Logging

### 2.1 Log Format

Structured JSON logging using Logback with Logstash encoder:

```json
{
  "timestamp": "2026-11-23T10:15:30.123Z",
  "level": "INFO",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.checkout.bff.application.service.CreatePreCheckoutService",
  "message": "Pre-checkout completed successfully",
  "context": {
    "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "cartId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "productSlugs": ["basic", "premium"],
    "durationMs": 45
  }
}
```

### 2.2 Required Fields

| Field | Description |
|-------|-------------|
| `timestamp` | ISO 8601 UTC timestamp |
| `level` | ERROR, WARN, INFO, DEBUG |
| `logger` | Fully qualified class name |
| `message` | Human-readable message |
| `correlationId` | Request correlation ID (MDC) |
| `spanId` | OpenTelemetry span ID (when tracing enabled) |
| `traceId` | OpenTelemetry trace ID (when tracing enabled) |

### 2.3 Correlation ID Propagation

```java
@Component
public class CorrelationIdFilter implements Filter {
    
    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String correlationId = ((HttpServletRequest) request).getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put(MDC_KEY, correlationId);
        ((HttpServletResponse) response).setHeader(HEADER_NAME, correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

### 2.4 Log Levels by Component

| Component | Level | Rationale |
|-----------|-------|-----------|
| Controllers | INFO | Request/response summary |
| Services | INFO | Business operation milestones |
| Repositories | DEBUG | Query execution details |
| External Clients | INFO | Request/response (sanitized) |
| Messaging | INFO | Publish/consume events |
| Security | WARN | Auth failures, suspicious activity |

### 2.5 Sensitive Data Masking

Never log:
- Full card numbers (mask: `****1234`)
- CVV values
- Passwords/tokens
- Full payment details

Use structured logging with explicit field inclusion.

---

## 3. Metrics (Micrometer + Prometheus)

### 3.1 Key Metrics

#### HTTP Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `http.server.requests` | Timer | Request latency, count, error rate |
| `http.server.requests.active` | Gauge | Active request count |

#### Business Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `checkout.precheckout.requests` | Counter | Pre-checkout requests total |
| `checkout.precheckout.success` | Counter | Successful pre-checkouts |
| `checkout.precheckout.errors` | Counter | Failed pre-checkouts by error code |
| `checkout.cart.created` | Counter | Carts created |
| `checkout.cart.paid` | Counter | Carts paid |
| `checkout.payment.declined` | Counter | Declined payments |
| `checkout.sync.requests` | Counter | Sync checkout requests total |
| `checkout.sync.success` | Counter | Successful sync checkouts |
| `checkout.sync.errors` | Counter | Failed sync checkouts by error code |
| `checkout.async.requests` | Counter | Async checkout requests total |
| `checkout.async.success` | Counter | Successful async checkouts |

#### Cache Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `cache.hits` | Counter | Cache hits |
| `cache.misses` | Counter | Cache misses |
| `cache.hit.ratio` | Gauge | Hit ratio (hits/(hits+misses)) |

#### External Service Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `external.service.latency` | Timer | Product/Payment service call latency |
| `external.service.errors` | Counter | External service errors by type |

#### Database Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `hikaricp.connections.active` | Gauge | Active DB connections |
| `hikaricp.connections.idle` | Gauge | Idle DB connections |
| `hikaricp.connections.pending` | Gauge | Threads waiting for connection |

#### Messaging Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `rabbitmq.messages.published` | Counter | Messages published |
| `rabbitmq.messages.consumed` | Counter | Messages consumed |
| `rabbitmq.messages.requeued` | Counter | Messages requeued |
| `rabbitmq.messages.dlq` | Counter | Messages in DLQ |

### 3.2 Prometheus Endpoint

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  prometheus:
    metrics:
      export:
        enabled: true
```

Access: `GET /actuator/prometheus`

### 3.3 Custom Metrics Example

```java
@Service
@RequiredArgsConstructor
public class CreatePreCheckoutService {
    
    private final MeterRegistry meterRegistry;
    private final Counter preCheckoutRequests;
    private final Counter preCheckoutSuccess;
    private final Counter preCheckoutErrors;
    private final Timer preCheckoutDuration;
    
    @PostConstruct
    void init() {
        preCheckoutRequests = Counter.builder("checkout.precheckout.requests")
            .register(meterRegistry);
        preCheckoutSuccess = Counter.builder("checkout.precheckout.success")
            .register(meterRegistry);
        preCheckoutErrors = Counter.builder("checkout.precheckout.errors")
            .register(meterRegistry);
        preCheckoutDuration = Timer.builder("checkout.precheckout.duration")
            .register(meterRegistry);
    }
    
    public PreCheckoutResponse execute(CreatePreCheckoutCommand command) {
        preCheckoutRequests.increment();
        
        return preCheckoutDuration.record(() -> {
            try {
                var response = doExecute(command);
                preCheckoutSuccess.increment();
                return response;
            } catch (Exception ex) {
                preCheckoutErrors.increment(Tag.of("error", ex.getClass().getSimpleName()));
                throw ex;
            }
        });
    }
}
```

---

## 4. Distributed Tracing

### 4.1 OpenTelemetry Configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}
```

### 4.2 Span Attributes

| Attribute | Description |
|-----------|-------------|
| `http.method` | HTTP method |
| `http.url` | Full request URL |
| `http.status_code` | Response status |
| `http.route` | Controller mapping |
| `db.statement` | SQL query (sanitized) |
| `messaging.system` | "rabbitmq" |
| `messaging.destination` | Queue/exchange name |
| `checkout.cart_id` | Cart identifier |
| `checkout.checkout_id` | Checkout identifier |

### 4.3 Trace Context Propagation

- HTTP: `traceparent`, `tracestate` headers
- Messaging: `traceparent` in message headers
- Logging: `traceId`, `spanId` in MDC

---

## 5. Health Checks

### 5.1 Liveness Probe

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessstate:
      enabled: true
```

**Endpoint**: `GET /actuator/health/liveness`  
**Purpose**: Kubernetes liveness probe - process is running

### 5.2 Readiness Probe

```yaml
management:
  health:
    readinessstate:
      enabled: true
  endpoint:
    health:
      probes:
        enabled: true
```

**Endpoint**: `GET /actuator/health/readiness`  
**Checks**:
- Database connectivity
- Redis connectivity
- RabbitMQ connectivity
- External service reachability (optional)

### 5.3 Health Indicator Details

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
    redis:
      enabled: true
    rabbit:
      enabled: true
```

---

## 6. Alerting Rules (Prometheus)

### 6.1 Critical Alerts

| Alert | Expression | Severity | Description |
|-------|------------|----------|-------------|
| `ServiceDown` | `up{job="checkout-bff"} == 0` | Critical | Application not responding |
| `HighErrorRate` | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05` | Critical | >5% 5xx errors |
| `DatabaseDown` | `hikaricp_connections_active == 0` | Critical | No DB connections |

### 6.2 Warning Alerts

| Alert | Expression | Severity | Description |
|-------|------------|----------|-------------|
| `HighLatency` | `histogram_quantile(0.95, http_server_requests_seconds_bucket) > 2` | Warning | p95 > 2s |
| `LowCacheHitRatio` | `cache_hit_ratio < 0.7` | Warning | Cache hit ratio < 70% |
| `DLQMessages` | `rabbitmq_messages_dlq > 0` | Warning | Messages in DLQ |
| `OutboxLag` | `outbox_events_pending > 100` | Warning | Outbox relay lag |

---

## 7. Audit Logging

### 7.1 Auditable Events

| Event | Fields |
|-------|--------|
| Cart Created | cartId, productSlugs, totalAmount, userId, correlationId |
| Cart Updated | cartId, changes, userId, correlationId |
| Checkout Initiated | cartId, checkoutId, paymentMethod, amount, correlationId |
| Payment Processed | checkoutId, status, amount, paymentMethod, correlationId |
| Payment Declined | checkoutId, reason, correlationId |

### 7.2 Audit Log Format

```json
{
  "eventType": "CART_CREATED",
  "timestamp": "2026-11-23T10:15:30.123Z",
  "actor": "frontend-user-123",
  "resource": "cart:a1b2c3d4-e5f6",
  "action": "CREATE",
  "outcome": "SUCCESS",
  "details": {
    "productSlugs": ["basic", "premium"],
    "totalAmount": 59.80,
    "currency": "BRL"
  },
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 8. Traceability

| Observability Element | Functional Requirement | Technical Spec |
|----------------------|------------------------|----------------|
| Structured logging | NFR: Observability | Architecture §10 |
| Correlation ID | NFR: Observability | Architecture §10, Error Handling §7 |
| Metrics (Prometheus) | NFR: Observability | Architecture §10 |
| Distributed tracing | NFR: Observability | Architecture §10 |
| Health checks | NFR: Reliability | Architecture §10 |
| Audit logging | NFR: Security/Compliance | This doc §7 |
| Alerting rules | NFR: Reliability | This doc §6 |