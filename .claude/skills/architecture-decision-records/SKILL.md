---
name: architecture-decision-records
description: Write and maintain Architecture Decision Records. Use when selecting or changing a technology, integration pattern, persistence approach, deployment model or architectural constraint.
---

# Architecture Decision Records

Use a short, durable ADR for each material decision.

## Required sections
- Title and ID (`ADR-*`)
- Status: proposed, accepted, rejected, superseded
- Date (only if known; do not fabricate)
- Context and problem
- Decision drivers and constraints
- Options considered, including "do nothing" where relevant
- Decision and rationale
- Consequences: positive, negative and operational
- Risks and mitigations
- Related requirements and decisions

## Rules
- Compare options against the same criteria.
- Separate facts from assumptions.
- Do not present a technology preference as a requirement.
- Do not select a message broker, database or cloud service without context and trade-off analysis.
- Supersede old ADRs instead of erasing their history.
