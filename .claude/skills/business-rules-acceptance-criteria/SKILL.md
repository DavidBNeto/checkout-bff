---
name: business-rules-acceptance-criteria
description: Model business rules and write testable acceptance criteria. Use when documenting checkout behavior, validation rules, state transitions or Given-When-Then scenarios.
---

# Business Rules and Acceptance Criteria

## Business rules
For each rule, record:
- Stable ID (`BR-*`), name and plain-language statement.
- Trigger and scope.
- Inputs and decision conditions.
- Outcome and side effects.
- Exceptions, precedence and boundary cases.
- Source/owner and status: confirmed, proposed or unresolved.

Separate policy from technical mechanism. For example, a payment eligibility rule is business policy; a queue retry policy is technical behavior.

## Acceptance criteria
Use observable outcomes. Prefer Given/When/Then when it improves clarity:
- Given: relevant state and preconditions.
- When: one action or event.
- Then: observable result and side effects.

Cover happy path, validation failure, authorization failure, duplicate request, timeout, dependency outage, concurrent requests and recovery where applicable.

Do not write criteria that prescribe internal implementation unless the requirement explicitly constrains it.
