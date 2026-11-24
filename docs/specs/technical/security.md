# Security Specification

**Status**: Confirmed  
**Version**: 1.0  
**Last Updated**: 2026-11-23  
**Owner**: Solution Architect

---

## 1. Overview

This document defines the security requirements and controls for the Checkout BFF, focusing on data protection, API security, and compliance with payment industry standards.

---

## 2. Security Principles

| Principle | Application |
|-----------|-------------|
| **Defense in Depth** | Multiple security layers: network, application, data |
| **Least Privilege** | Minimal permissions for each component |
| **Data Minimization** | Only collect/store necessary data |
| **Secure by Default** | Secure configurations out of the box |
| **Fail Secure** | Errors don't expose sensitive information |
| **Auditability** | All security-relevant events logged |

---

## 3. Data Protection

### 3.1 Payment Data Handling

| Data Type | Storage | Transmission | Logging |
|-----------|---------|--------------|---------|
| Full card number | NEVER | Encrypted (TLS 1.2+) | NEVER |
| CVV/CVC | NEVER | Encrypted (TLS 1.2+) | NEVER |
| Cardholder name | Encrypted at rest | Encrypted (TLS 1.2+) | Masked |
| Expiry date | Encrypted at rest | Encrypted (TLS 1.2+) | Masked |
| Last 4 digits | Allowed | Allowed | Allowed |
| Tokenized data | Allowed (token) | Allowed | Allowed |

### 3.2 Encryption Standards

| Context | Standard |
|---------|----------|
| Data in transit | TLS 1.2+ (TLS 1.3 preferred) |
| Data at rest (PII) | AES-256-GCM |
| Database encryption | Transparent Data Encryption (TDE) |
| Key management | External KMS (AWS KMS, HashiCorp Vault) |

### 3.3 Data Masking in Logs

```java
public class PaymentDataMasker {
    
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
    
    public static String maskCvv(String cvv) {
        return "***";
    }
    
    public static String maskHolder(String holder) {
        if (holder == null) return "****";
        String[] parts = holder.split(" ");
        return Arrays.stream(parts)
            .map(p -> p.charAt(0) + "****")
            .collect(Collectors.joining(" "));
    }
}
```

---

## 4. API Security

### 4.1 Authentication & Authorization

**Current**: No authentication (BFF trusts frontend/internal network)  
**Future**: OAuth2 Resource Server with JWT validation

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}
          jwk-set-uri: ${OAUTH2_JWK_SET_URI}
```

### 4.2 Input Validation

All request DTOs use Bean Validation:

```java
public class CreatePreCheckoutRequest {
    
    @NotNull(message = "Product slugs are required")
    @Size(min = 1, max = 50, message = "Must have 1-50 products")
    @ValidProductSlugs
    private List<@NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String> productSlugs;
    
    // getters/setters
}
```

### 4.3 Rate Limiting

```yaml
# Planned - Spring Cloud Gateway or Resilience4j
rate-limit:
  pre-checkout:
    requests-per-minute: 60
    burst: 10
  checkout:
    requests-per-minute: 30
    burst: 5
```

### 4.4 Security Headers

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(policy -> policy
                    .policy("geolocation=(), microphone=(), camera=()")))
            .csrf(csrf -> csrf.disable()) // Stateless API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .build();
    }
}
```

---

## 5. CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://checkout.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "X-Correlation-ID", "Idempotency-Key"));
    config.setExposedHeaders(List.of("X-Correlation-ID", "Location"));
    config.setMaxAge(3600L);
    
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

---

## 6. Dependency Security

### 6.1 Vulnerability Scanning

- **OWASP Dependency Check** in CI pipeline
- **GitHub Dependabot** alerts enabled
- **Fail build** on CVSS >= 7.0

### 6.2 Approved Dependencies

All dependencies from:
- Spring Boot BOM (managed versions)
- Maven Central (verified publishers)

---

## 7. Infrastructure Security

### 7.1 Network

| Component | Network Policy |
|-----------|----------------|
| BFF | Private subnet, no public IP |
| PostgreSQL | Private subnet, only BFF access |
| Redis | Private subnet, only BFF access |
| RabbitMQ | Private subnet, only BFF access |
| External Services | Egress via NAT Gateway |

### 7.2 Secrets Management

```yaml
# Never in code/config - use external secret manager
spring:
  datasource:
    password: ${DB_PASSWORD}  # Injected at runtime
  rabbitmq:
    password: ${RABBITMQ_PASSWORD}
  redis:
    password: ${REDIS_PASSWORD}
```

### 7.3 Container Security

```dockerfile
# Dockerfile security practices
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

USER appuser

# Read-only root filesystem
# No shell
# Minimal base image
```

---

## 8. Payment Industry Compliance

### 8.1 PCI DSS Scope

**BFF is NOT a PCI DSS entity** - it does not store, process, or transmit cardholder data directly.
- Card data handled by external payment gateway
- BFF only receives tokenized/last4 data

### 8.2 Data Retention

| Data | Retention | Disposal |
|------|-----------|----------|
| Cart (PENDING) | 30 minutes | Auto-expire |
| Cart (PAID) | 7 years | Archive + purge |
| Payment tokens | 7 years | Secure deletion |
| Audit logs | 7 years | Archive |

---

## 9. Security Testing

### 9.1 SAST
- SpotBugs / SonarQube in CI
- Custom rules for payment data patterns

### 9.2 DAST
- OWASP ZAP scan against running instance
- Focus on injection, auth bypass

### 9.3 Dependency Scanning
- OWASP Dependency Check
- Snyk / GitHub Dependabot

### 9.4 Penetration Testing
- Annual third-party pen test
- Focus on payment flows

---

## 10. Incident Response

### 10.1 Security Events to Monitor

| Event | Detection | Response |
|-------|-----------|----------|
| Multiple failed pre-checkouts | Metrics alert | Investigate abuse |
| DLQ accumulation | Metrics alert | Check payment worker |
| Unusual traffic patterns | WAF/Rate limit | Block/Throttle |
| Dependency vulnerability | Dependabot | Patch within SLA |

### 10.2 Breach Notification

- **Internal**: Security team within 1 hour
- **Regulatory**: Per applicable law (GDPR 72h, PCI DSS immediate)
- **Customers**: Per contract/regulation

---

## 11. Traceability

| Security Control | Functional Requirement | Technical Spec |
|------------------|------------------------|----------------|
| No card storage | NFR: Security | Scope §6 |
| TLS encryption | NFR: Security | Architecture §9 |
| Input validation | FR-PRE-005 | Error Handling §5.2 |
| Security headers | NFR: Security | This doc §4.4 |
| CORS policy | NFR: Security | This doc §5 |
| Secrets management | NFR: Security | This doc §7.2 |
| Non-root container | NFR: Security | This doc §7.3 |
| Audit logging | NFR: Observability | Observability §7 |
| Dependency scanning | NFR: Security | This doc §6 |
| Sensitive data masking in responses | NFR: Security | API Contracts §2.3, §3.3 |
| Standardized error contract (no stack traces) | NFR: Security | API Contracts §2.4, Error Handling §2 |
| Correlation ID for tracing | NFR: Observability | API Contracts §2.5, §3.5 |