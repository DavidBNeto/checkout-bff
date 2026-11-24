---
name: requirements-traceability
description: Create and audit traceability between requirements, architecture, API/event contracts, code and tests. Use when planning changes or checking coverage and impact.
---

# Requirements Traceability

## Identifier conventions
Use existing project conventions first. If none exist:
- `FR-*`: functional requirement
- `NFR-*`: non-functional requirement
- `BR-*`: business rule
- `API-*`: API operation/contract
- `EVT-*`: event or message contract
- `ADR-*`: architecture decision
- `TC-*`: test case

## Traceability model
Connect each requirement to:
1. Acceptance criteria.
2. Relevant architecture component/diagram or ADR.
3. API, event or data contract where applicable.
4. Implementation location when known.
5. Automated or manual test evidence.

## Audit
Find requirements with no acceptance criteria, specs with no source requirement, contracts not referenced by design, and requirements without test evidence.

Distinguish planned, implemented and verified. A link to a source file is not proof that behavior works; require test evidence.

## Change impact
For every changed requirement or contract, list downstream artifacts likely affected and explain why. Do not update unrelated documents just to create artificial links.
