# Spring-Music Modernisation — Coordinator Plan

## Dependency Graph

```
Challenge 0 (CLAUDE.md setup)
    ├─► Challenge 2 (pm-stories — The Stories)     ─┐  PARALLEL
    └─► Challenge 3 (architect-map — The Map)      ─┘
            ├─► Challenge 4 (tester-pin — The Pin) ─┐  PARALLEL
            └─► Challenge 9 (agentic-scouts)        ─┘
                    └─► Challenge 5 (dev-cut — The Cut)
                            ├─► Challenge 6 (dev-fence — The Fence)   ─┐  PARALLEL
                            └─► Challenge 8 (ops-weekend — The Weekend)─┘
                                        └─► Challenge 7 (quality-scorecard)
```

## Phase Overview

```
Phase 1:  [manual setup]
Phase 2:  [pm-stories] || [architect-map]            ← 2 agents in parallel
Phase 3:  [tester-pin] || [agentic-scouts]            ← 2 agents in parallel
Phase 4:  [dev-cut]
Phase 5:  [dev-fence]  || [ops-weekend]               ← 2 agents in parallel
Phase 6:  [quality-scorecard]
```

---

## Phase 1 — Foundation (manual)

**Challenge 0 — Three-Level CLAUDE.md Setup**

No agent. Human task: create the CLAUDE.md hierarchy and supporting config.

| File | Action |
|------|--------|
| `~/.claude/CLAUDE.md` | Create — personal preferences (tone, commit style) |
| `spring-music/CLAUDE.md` | Update — workshop conventions, service boundary rules |
| `spring-music/src/main/java/org/cloudfoundry/samples/music/CLAUDE.md` | Create — "You are in the legacy monolith root. Prefer extracting to new-service/ over adding here." |
| `spring-music/new-service/CLAUDE.md` | Create — "You are in the extracted catalog service. Never reference monolith internal packages." |
| `spring-music/.claude/commands/extract-service.md` | Create — step-by-step extraction playbook |
| `spring-music/.claude/commands/pin-behavior.md` | Create — runs characterization suite and reports deviations |
| `spring-music/.claude/commands/score-seam.md` | Create — invokes Scouts subagent for a named class/package |
| `spring-music/.claude/settings.json` | Create — stub for hook registration |

**Gate:** All CLAUDE.md levels in place and `.claude/settings.json` present before spawning any agent.

---

## Phase 2 — Stories + Map (2 agents in parallel)

Spawn both agents in a single coordinator message.

### Challenge 2 — `pm-stories`

- **Input:** Current monolith
- **Output:** `docs/stories.md`
- Stories: Browse catalog, Add/update album, Delete album, View recommendations, Play history, Backend health
- Format: Given/When/Then, 2–3 ACs per story, explicit "open questions" section capturing stakeholder disagreements

### Challenge 3 — `architect-map`

- **Input:** Current monolith
- **Output:** `docs/adr/001-service-decomposition.md`
- Sections: Status, Context, Decision (3 services in extraction order), seam ranking table, what we chose not to do, three-level CLAUDE.md rationale
- Seam ranking columns: seam, coupling (1–5), test coverage (%), data-model tangle (1–5), business criticality (H/M/L), overall extraction risk

**Gate:** ADR `001-service-decomposition.md` written with completed seam ranking table.

---

## Phase 3 — Pin + Scouts (2 agents in parallel)

Spawn both agents in a single coordinator message after Phase 2 completes.

### Challenge 4 — `tester-pin`

- **Input:** Current monolith + ADR seam ranking from Phase 2
- **Output:** Test classes under `src/test/java/org/cloudfoundry/samples/music/characterization/`

| Test class | Pins |
|------------|------|
| `AlbumCrudCharacterizationTest.java` | All 5 CRUD operations; exact HTTP status codes; response body field names and types |
| `AlbumDataShapeTest.java` | JSON field presence including `lastPlayedBy`, `playCount`; null handling |
| `RecommendationCharacterizationTest.java` | Recommendations identical regardless of user (current bug — pinned) |
| `AuditCharacterizationTest.java` | Every save writes exactly one audit row with albumId and timestamp |
| `ProfileSwitchingTest.java` | H2 profile boots and seeds 12 albums; wrong-profile combo throws `IllegalStateException` |

- All classes annotated `@Tag("characterization")`
- Assertions pin field names, not just values

### Challenge 9 — `agentic-scouts`

- **Input:** ADR seam ranking from Phase 2 (needed for comparison)
- **Output:** `docs/scout-report.md`
- Fan-out: one Task subagent per seam (AlbumCatalog, UserPreference, RecommendationEngine, AuditService)
- Each subagent receives full explicit context: package path, class list, dependency graph excerpt, scoring rubric
- Subagents do NOT inherit coordinator context
- Coordinator aggregates verdicts, compares against human ADR ranking, reports agreements/disagreements

**Gate:** Characterization suite passing under `./gradlew test --tests "*.characterization.*"`.

---

## Phase 4 — Extract the Service (single agent)

**Challenge 5 — `dev-cut`**

- **Input:** Green characterization suite from Phase 3
- **Agent:** `dev-cut`
- **Output:** New `catalog-service/` module + strangler proxy in monolith

```
catalog-service/
  build.gradle
  src/main/java/org/cloudfoundry/catalog/
    CatalogApplication.java
    api/AlbumResource.java          (clean DTO — no JPA annotations, no user-tracking fields)
    api/CatalogController.java      (port 8081)
    domain/CatalogAlbum.java        (title, artist, releaseYear, genre, trackCount only)
    repository/CatalogAlbumRepository.java
  src/test/java/.../
    CatalogContractTest.java        (@Tag("contract"))
```

- `AlbumController.java` in monolith gets a `catalogServiceClient` (RestTemplate) proxy for GET requests when `catalog.service.enabled=true`; falls back to local repo when false.

**Gate:** `./gradlew test` green for both `@Tag("characterization")` and `@Tag("contract")` on the same commit.

---

## Phase 5 — Fence + Runbook (2 agents in parallel)

Spawn both agents in a single coordinator message after Phase 4 completes.

### Challenge 6 — `dev-fence`

- **Input:** Extracted `catalog-service/` from Phase 4
- **Output:**
  - `catalog-service/src/main/java/.../acl/AlbumTranslator.java` — only entry point for cross-boundary data
  - `catalog-service/src/test/java/.../fence/BoundaryLeakTest.java` — fails if `lastPlayedBy`, `playCount`, `recommendedFor`, or `_class` appear in API response
  - `spring-music/.claude/hooks/fence-check.sh` — PreToolUse hook blocking monolith field names in catalog-service edits
  - `docs/adr/002-fence-strategy.md`
  - `.claude/settings.json` updated to register the hook

### Challenge 8 — `ops-weekend`

- **Input:** Extracted `catalog-service/` from Phase 4
- **Output:** `docs/runbook/cutover.md`
- Sections: pre-cutover checklist, traffic migration steps, verification gates, rollback triggers, Mermaid decision tree, rehearsal notes
- Gates: error rate < 0.1%, p99 latency within 20% of baseline

**Gate (for Phase 6):** `BoundaryLeakTest` passing; `fence-check.sh` hook registered and smoke-tested.

---

## Phase 6 — Eval Harness (single agent)

**Challenge 7 — `quality-scorecard`**

- **Input:** Fence in place + golden set informed by ADRs and scout report
- **Agent:** `quality-scorecard`
- **Output:**

```
eval/
  golden_set.json     labeled correct/incorrect seam proposals
  run_eval.py         invokes Claude API, scores proposals, reports metrics
  ci_gate.sh          exits non-zero if false-confidence rate > threshold
```

- Metrics: boundary correctness (%), behavior preservation (characterization pass rate), false-confidence rate (%)
- CI threshold: false-confidence rate < 20%

---

## Gate Conditions Summary

| Before Phase | Gate condition |
|---|---|
| Phase 2 | CLAUDE.md hierarchy complete; `.claude/settings.json` stub present |
| Phase 3 | `docs/adr/001-service-decomposition.md` written with seam ranking table |
| Phase 4 | `./gradlew test --tests "*.characterization.*"` green |
| Phase 5 | `./gradlew test` green for `@Tag("characterization")` AND `@Tag("contract")` on same commit |
| Phase 6 | `BoundaryLeakTest` passing; `fence-check.sh` registered and tested |

---

## Verification Commands

```bash
# All tests (smoke + characterization + contract)
./gradlew test

# Characterization suite only
./gradlew test --tests "*.characterization.*"

# Boundary leak test only
./gradlew test --tests "*.fence.*"

# Eval scorecard
python eval/run_eval.py

# Hook smoke test (should be blocked)
# Attempt to write "lastPlayedBy" into catalog-service/ — hook must fire and reject
```
