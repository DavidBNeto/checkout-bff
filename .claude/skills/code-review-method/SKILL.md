---
name: code-review-method
description: Perform evidence-based reviews of backend code changes. Use when reviewing a diff for correctness, security, maintainability, performance or specification compliance.
---

# Code Review Method

Review the diff and surrounding code. Do not review only isolated changed lines.

## Priorities
1. Correctness and business behavior.
2. Security and sensitive-data handling.
3. Data integrity, transactions and concurrency.
4. API/event contract compatibility.
5. Failure handling, retries and idempotency.
6. Performance and resource lifecycle.
7. Maintainability and consistency with repository conventions.
8. Tests and documentation.

## Findings
Each finding must include severity, file/line, concrete scenario, impact and suggested fix. Avoid speculative findings; explain the evidence and conditions.

Do not make code changes during review. Do not report style preferences as bugs. If no findings exist, state the scope reviewed and residual testing limitations.
