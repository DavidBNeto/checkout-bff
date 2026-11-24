# Backend SDD Project Instructions

## Project context
This repository contains a backend-only Java/Spring Boot Checkout BFF.
Use the repository as the source of truth: inspect existing code, specs, build files,
configuration and tests before proposing or changing anything.

The architecture may include REST APIs, persistence, Redis, asynchronous processing,
external integrations, containers and cloud infrastructure. Do not assume a specific
message broker, cloud provider, database, deployment platform or payment provider unless
the repository or an approved decision explicitly identifies it.

## Development approach
Follow Spec-Driven Development (SDD):
1. Discover the current state before creating or changing artifacts.
2. Identify the relevant requirements and approved specifications.
3. Report missing, ambiguous or conflicting information.
4. Propose changes and identify affected artifacts.
5. Implement only against approved requirements and technical decisions.
6. Validate with automated checks and report evidence.

Treat approved specifications as the baseline, not as immutable truth. If implementation
reveals a defect or infeasibility, raise a change proposal. Do not silently rewrite an
approved requirement or contract to make implementation easier.

## Agent delegation
Use the specialist subagents when their expertise materially improves the task:
- `business-analyst`: business needs, functional requirements, rules, stories and acceptance criteria.
- `solution-architect`: target architecture, boundaries, interactions and architecture decisions.
- `technical-analyst`: technical design, API/event/data contracts and implementation-ready specifications.
- `architecture-reviewer`: independent architecture challenge and risk review.
- `documentation-reviewer`: cross-document consistency, completeness and traceability.
- `java-engineer`: Java/Spring Boot implementation.
- `qa-engineer`: test strategy, test design and automated tests.
- `code-reviewer`: independent, read-only code review.
- `devops-engineer`: docker and infrastructure management.

The main Claude Code session is the coordinator. Do not create another orchestrator agent.

## Authority and change control
- Business Analyst proposes functional requirements; the human approves business behavior.
- Solution Architect proposes architecture; the human approves material architectural trade-offs.
- Technical Analyst proposes contracts and technical details; the human approves externally observable contract changes.
- Implementers may identify gaps but must not silently change approved specs.
- Reviewers report findings; they do not approve their own work.
- Do not claim a requirement is implemented without pointing to code and test evidence.

## Shared documentation conventions
- Prefer updating the canonical existing document over creating duplicates.
- Use stable IDs where traceability is useful: `FR-*`, `NFR-*`, `BR-*`, `API-*`,
  `EVT-*`, `ADR-*`, `TC-*`.
- Mark statements as confirmed, proposed, assumed or unresolved.
- Record decisions and alternatives in ADRs.
- Keep Mermaid diagrams as source files or fenced Mermaid blocks, not screenshots.
- Keep OpenAPI documents machine-validatable.
- Use consistent names across requirements, API schemas, events, code and tests.
- Include failure paths, timeouts, retries, idempotency and observability where relevant.

## Engineering constraints
- Follow existing project conventions and dependency versions.
- Prefer the smallest change that satisfies the approved specification.
- Do not add dependencies, infrastructure, endpoints, queues, database tables or configuration
  without explaining why and obtaining approval when the change is material.
- Never hardcode secrets or place credentials in documentation, tests or source.
- Avoid logging payment data, credentials, tokens or other sensitive information.
- Do not assume exactly-once message delivery. Explicitly design for duplicate delivery
  and idempotent processing where messaging is involved.
- The stack is defined by: Postgrtes db, redis for caching, java 21 for development, maven for building, spring boot, RabbitMQ for async events, JUnit and Mockito for testing and docker execution.
- Do not claim distributed transactions or strong consistency without an explicit design.
- Never return database ids from the BFF

## Validation and reporting
Run the checks appropriate to the change (for example build, unit tests, integration tests,
OpenAPI validation, diagram rendering, Docker build or infrastructure validation).
Do not claim checks passed unless they were actually executed successfully.

At the end of each task, report:
1. What changed.
2. Which requirements/specifications were used.
3. Validation performed and results.
4. Decisions or assumptions made.
5. Remaining risks, questions and follow-up work.