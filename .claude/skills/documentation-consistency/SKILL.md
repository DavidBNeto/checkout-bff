---
name: documentation-consistency
description: Audit functional, architecture and technical documentation for contradictions, gaps and stale references. Use before approving a documentation set or after specification changes.
---

# Documentation Consistency Review

## Review dimensions
- Terminology and naming consistency.
- Requirement-to-acceptance-criteria coverage.
- API and event contract agreement with diagrams.
- State machine agreement with sequence flows.
- Error/status behavior agreement across PRD, API and implementation specs.
- Data ownership and persistence agreement.
- Security, privacy and observability coverage.
- Versioning, links, IDs and examples validity.

## Method
1. Read all relevant canonical artifacts, not just the changed file.
2. Build a compact list of claims and their sources.
3. Identify contradictions, omissions, ambiguity and stale content.
4. Classify findings as blocker, major, minor or observation.
5. Cite exact file paths and headings/line ranges when available.
6. Suggest the smallest correction and identify the owner.

Do not silently rewrite documents during an audit. Return findings and proposed changes for the responsible agent/human to approve.
