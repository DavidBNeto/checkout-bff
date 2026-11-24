---
name: sdd-workflow
description: Apply the project's Spec-Driven Development lifecycle. Use when planning features, moving from requirements to design and implementation, or checking whether work is ready to proceed.
---

# SDD Workflow

## Purpose
Keep requirements, design, implementation and tests connected. Specifications are working engineering artifacts, not paperwork created after coding.

## Procedure
1. Inspect `CLAUDE.md`, repository structure, current specs, ADRs, code and tests.
2. Classify the request: discovery, requirements, architecture, technical design, implementation, validation or change.
3. Identify the canonical artifacts and owners affected.
4. Check whether prerequisites are approved. If not, produce a proposal and list unresolved questions.
5. Break work into small deliverables with explicit inputs, outputs and dependencies.
6. Delegate only independent work. Do not parallelize tasks that write the same file or depend on unapproved decisions.
7. Validate the output against acceptance criteria and linked contracts.
8. Update traceability and summarize changes, evidence and open questions.

## Change control
- Never silently convert assumptions into requirements.
- Never silently change an approved API or event contract.
- When implementation exposes a specification gap, stop at the boundary, describe the issue and propose a change.
- Keep old decisions discoverable; supersede ADRs rather than rewriting history.

## Readiness gates
- Requirements ready: actors, trigger, outcome, rules, errors and acceptance criteria are sufficiently clear.
- Design ready: boundaries, interactions, data ownership, failure behavior and material decisions are documented.
- Implementation ready: relevant API/event/data contracts and acceptance criteria are approved.
- Done: code, tests, documentation and validation evidence agree.
