# Plan: Spring-Music Modernization Workshop Challenges

## Context

Spring-music is a clean Spring Boot 2.4 / Gradle 6 sample app demonstrating swappable persistence backends via Spring profiles. The existing code is architecturally tidy (strategy pattern, ~5 packages, one domain object). The workshop uses it as a canvas for nine challenges that stress three Claude Code cert domains: Config, Context Management, and Agentic Architecture.

Because the app is *too clean*, we must first synthesize realistic technical debt (The Patient) before the remaining challenges become meaningful. Every subsequent challenge depends on that uglified baseline.

---

## Challenge Map & Execution Order

```
0. Three-Level CLAUDE.md Setup  (Config — prerequisite for all)
1. The Patient  (generate legacy monolith)
2. The Stories  (user stories + acceptance criteria)
3. The Map      (decomposition ADR)
4. The Pin      (characterization tests)
5. The Cut      (extract first service)
6. The Fence    (anti-corruption layer + hook)
7. The Scorecard (eval harness)
8. The Weekend  (cutover runbook)
9. The Scouts   (Task subagents, parallel risk scoring)
```

---

## Challenge 0 — Three-Level CLAUDE.md Setup

**Goal:** Establish the three-level context hierarchy so every subsequent challenge inherits the right instructions.

### Files to create / edit

| Level | Path | Purpose |
|-------|------|---------|
| User | `~/.claude/CLAUDE.md` | Personal preferences (tone, language, commit style) |
| Project | `spring-music/CLAUDE.md` | Existing file — add workshop conventions, service boundary rules |
| Directory (monolith) | `spring-music/src/main/java/org/cloudfoundry/samples/music/CLAUDE.md` | "You are in the legacy monolith root. Prefer extracting to new-service/ over adding here." |
| Directory (new service) | `spring-music/new-service/CLAUDE.md` | "You are in the extracted catalog service. Never reference monolith internal packages." |

### Custom commands to add in `spring-music/.claude/commands/`

- `extract-service.md` — step-by-step extraction playbook (takes `<seam-name>` arg)
- `pin-behavior.md` — runs characterization test suite and reports deviations
- `score-seam.md` — invokes Scouts subagent for a named class/package

---

## Challenge 1 — The Patient (Legacy Monolith Generation)

**Goal:** Add realistic ugliness so downstream challenges have teeth.

### What to add to the existing codebase

**New classes** under `src/main/java/org/cloudfoundry/samples/music/`:

| Class | Problem it introduces |
|-------|-----------------------|
| `service/MusicCatalogFacade.java` | God class: CRUD + recommendation + audit + user-pref logic, ~400 lines |
| `service/RecommendationEngine.java` | Calls back into `MusicCatalogFacade` → circular dependency via `@Lazy` |
| `service/AuditService.java` | "Trigger" simulation: intercepts every save, writes to audit table via raw JDBC |
| `domain/UserPreference.java` | New entity whose fields bleed into `Album` (adds `lastPlayedBy`, `playCount` to Album) |
| `web/AlbumController.java` | Refactor to call `MusicCatalogFacade` instead of `CrudRepository` directly |
| `config/DataInitializationService.java` | Mixes seed data logic, schema migration, and cache warming in one `@PostConstruct` |

**Changes to existing files:**
- `Album.java` — add `lastPlayedBy` (String), `playCount` (int), `recommendedFor` (String) — JPA columns leaking user-tracking concerns into the catalog entity
- `application.yml` — add audit datasource pointing to same H2 URL (simulating shared DB anti-pattern)

**Result:** The app still runs and passes `ApplicationTests`, but now has the canonical monolith smells: god class, circular dep, shared DB, business logic in initializer, domain model bloat.

---

## Challenge 2 — The Stories (PM)

**Goal:** User stories with sharp acceptance criteria; capture stakeholder disagreements explicitly.

### Stories to write (file: `spring-music/docs/stories.md`)

| # | Story | Key disagreements to capture |
|---|-------|-------------------------------|
| S1 | Browse catalog | Search vs. filter-only; pagination size |
| S2 | Add / update album | Who can edit — all users or admin role? |
| S3 | Delete album | Soft delete vs. hard delete; audit trail requirement |
| S4 | View recommendations | Personalized vs. genre-based; cold-start handling |
| S5 | Play history | Per-user vs. global; GDPR retention limit |
| S6 | Backend health | Ops story — which DB is live; failover indicator |

Each story: Given/When/Then format, 2–3 ACs, explicit "open question" section.

---

## Challenge 3 — The Map (Decomposition ADR)

**Goal:** ADR naming seams, ranked by extraction risk, with "what we chose not to do."

### File: `spring-music/docs/adr/001-service-decomposition.md`

**Sections:**

1. **Status:** Proposed
2. **Context:** Current monolith pain points (from The Patient)
3. **Decision:** Extract three services in this order:
   - *Album Catalog Service* (lowest risk — clean repository abstraction already exists)
   - *User Preference Service* (medium — new entity, minimal existing coupling)
   - *Recommendation Engine Service* (highest risk — circular dep, shared DB, no tests)
4. **Seam ranking table:** columns = seam, coupling score (1–5), test coverage (%), data-model tangle (1–5), business criticality (H/M/L), overall extraction risk
5. **What we chose not to do:** extract `AuditService` (deferred — requires event streaming), split `MusicCatalogFacade` in one step (too risky without characterization tests)
6. **Three-level CLAUDE.md rationale:** why the monolith dir gets a "prefer extraction" prompt vs. why the service boundary enforcement is a PreToolUse hook (deterministic enforcement) vs. a prompt (preference guidance)

---

## Challenge 4 — The Pin (Characterization Tests)

**Goal:** Behavior-pinning tests against the uglified monolith before any refactor.

### Files to create under `src/test/java/org/cloudfoundry/samples/music/characterization/`

| Test class | Pins |
|------------|------|
| `AlbumCrudCharacterizationTest.java` | All 5 CRUD operations; exact HTTP status codes; response body field names and types |
| `AlbumDataShapeTest.java` | JSON field presence (including `lastPlayedBy`, `playCount` — bugs included); null handling for optional fields |
| `RecommendationCharacterizationTest.java` | Recommendation endpoint returns same albums regardless of user (current bug — pins it) |
| `AuditCharacterizationTest.java` | Every save writes exactly one audit row; audit row contains albumId and timestamp |
| `ProfileSwitchingTest.java` | H2 profile boots and seeds 12 albums; wrong-profile combo throws `IllegalStateException` |

**Annotation convention:** `@Tag("characterization")` on all classes so they can be run separately from new contract tests.

**Assertion style:** `assertThat(response.body).contains("lastPlayedBy")` — pins field names, not just values, so a rename breaks the test with a useful message.

---

## Challenge 5 — The Cut (Extract Album Catalog Service)

**Goal:** Album Catalog Service live alongside monolith; both test suites green on same commit.

### New module: `spring-music/catalog-service/`

Structure:
```
catalog-service/
  build.gradle           (Spring Boot 2.4, H2/JPA only)
  src/main/java/org/cloudfoundry/catalog/
    CatalogApplication.java
    api/AlbumResource.java         (clean DTO — no JPA annotations, no user-tracking fields)
    api/CatalogController.java     (port 8081)
    domain/CatalogAlbum.java       (pure domain — title, artist, releaseYear, genre, trackCount)
    repository/CatalogAlbumRepository.java
  src/test/java/.../
    CatalogContractTest.java       (Spring MockMvc — verifies API shape)
```

**Strangler fig in monolith:** `web/AlbumController.java` gets a `catalogServiceClient` (RestTemplate) that proxies GET requests to the catalog service when `catalog.service.enabled=true` property is set; falls back to local repository when false. This lets the monolith and service coexist.

**Test strategy:**
- Characterization suite (`@Tag("characterization")`) must still pass after extraction
- `CatalogContractTest` added as `@Tag("contract")`
- Both run in `./gradlew test` via tag inclusion in `build.gradle`

---

## Challenge 6 — The Fence (Anti-Corruption Layer)

**Goal:** Monolith's internal model cannot leak into the catalog service's public API.

### ACL translation layer: `catalog-service/src/main/java/.../acl/`

- `AlbumTranslator.java` — maps monolith `Album` (with JPA annotations, `lastPlayedBy`, etc.) to `AlbumResource` DTO; enforced as the only entry point for cross-boundary data

### Fence test: `catalog-service/src/test/java/.../fence/BoundaryLeakTest.java`

```java
// Scans catalog-service API response JSON for any monolith-internal field names.
// Fails loudly if "lastPlayedBy", "playCount", "recommendedFor", or "_class" appear.
@Test void monolithFieldNamesMustNotLeakIntoApiResponse() { ... }
```

### PreToolUse hook (Claude Code Config)

File: `spring-music/.claude/hooks/fence-check.sh`

```bash
# Fires before any Edit or Write tool call targeting catalog-service/
# Rejects if the diff contains any of: lastPlayedBy, playCount, recommendedFor, _class
# Exit 1 with message: "Boundary violation: monolith field '$field' in catalog-service"
```

Registered in `spring-music/.claude/settings.json`:
```json
{
  "hooks": {
    "PreToolUse": [{ "matcher": "Edit|Write", "hooks": [{"type": "command", "command": ".claude/hooks/fence-check.sh"}] }]
  }
}
```

ADR `002-fence-strategy.md` explains: hard block is a hook (deterministic, cannot be reasoned around), preference guidance is a prompt ("prefer the new service for album reads") because it has legitimate exceptions.

---

## Challenge 7 — The Scorecard (Eval Harness)

**Goal:** Reproducible metrics for LLM-driven refactoring quality; runs in CI.

### File: `spring-music/eval/`

```
eval/
  golden_set.json         labeled correct/incorrect seam proposals
  run_eval.py             invokes Claude API, scores proposals, reports metrics
  ci_gate.sh              exits non-zero if false-confidence rate > threshold
```

**Golden set structure (`golden_set.json`):**

```json
[
  { "module": "JpaAlbumRepository", "label": "correct_seam", "reason": "clean interface boundary, no circular deps" },
  { "module": "MusicCatalogFacade", "label": "incorrect_seam", "reason": "god class — must split before extracting" },
  ...
]
```

**Metrics tracked by `run_eval.py`:**
1. **Boundary correctness** — % of seam proposals matching golden labels
2. **Behavior preservation** — characterization suite pass rate after Claude's refactor
3. **False-confidence rate** — % of wrong proposals where Claude reported confidence ≥ 0.8

**CI integration:** `eval/ci_gate.sh` is a step in `build.gradle`'s `check` task (or a separate GitHub Actions job). Threshold: false-confidence rate < 20%.

---

## Challenge 8 — The Weekend (Cutover Runbook)

**Goal:** Ops-executable 3am runbook with rollback triggers and a decision tree.

### File: `spring-music/docs/runbook/cutover.md`

**Sections:**
1. Pre-cutover checklist (characterization suite green, catalog-service health check, DB snapshot)
2. Traffic migration steps (flip `catalog.service.enabled=true` per environment)
3. Verification gates (error rate < 0.1%, p99 latency within 20% of baseline)
4. Rollback triggers (any gate fails → immediate revert to monolith)
5. Decision tree (flowchart in Mermaid): "gate passes → proceed / gate fails → is it recoverable? → rollback or escalate"
6. Rehearsal notes (record of a dry-run with timing)

---

## Challenge 9 — The Scouts (Agentic Architecture)

**Goal:** Fan-out Task subagents, one per seam, independently scoring extraction risk; coordinator aggregates.

### File: `spring-music/.claude/commands/score-seam.md` (custom command)

**Coordinator logic:**

```
For each seam in [AlbumCatalog, UserPreference, RecommendationEngine, AuditService]:
  Spawn Task subagent with:
    - explicit scope: package path, list of classes, dependency graph excerpt
    - scoring rubric: coupling (1-5), test coverage (%), data-model tangle (1-5), business criticality
    - instruction: "Return a JSON verdict only. Do not read files outside your assigned package."
  Collect verdicts
Aggregate → ranked list with confidence intervals
Compare against human ADR ranking from Challenge 3
Report agreements, disagreements, and explanations
```

**Key design constraint:** Each subagent prompt includes its full context explicitly (package contents, dependency list) — subagents do NOT inherit coordinator context. This is the explicit-context pattern required by the cert domain.

**Output:** `spring-music/docs/scout-report.md` — ranked seams with per-subagent scores, aggregated rank, diff vs. human ADR, and where/why they disagree.

---

## Verification

- `./gradlew test` — all tests green (smoke + characterization + contract)
- `./gradlew test --tests "*.characterization.*"` — pin suite only
- `./gradlew test --tests "*.fence.*"` — boundary leak test
- `python eval/run_eval.py` — scorecard metrics
- Hook test: attempt to write `lastPlayedBy` into `catalog-service/` → hook fires, edit blocked

---

## Files Summary

| Challenge | New / Modified Files |
|-----------|----------------------|
| 0 | `~/.claude/CLAUDE.md`, `spring-music/CLAUDE.md`, `...music/CLAUDE.md`, `new-service/CLAUDE.md`, `.claude/commands/*.md`, `.claude/settings.json` |
| 1 | `service/MusicCatalogFacade.java`, `service/RecommendationEngine.java`, `service/AuditService.java`, `domain/UserPreference.java`, `Album.java`, `AlbumController.java`, `config/DataInitializationService.java`, `application.yml` |
| 2 | `docs/stories.md` |
| 3 | `docs/adr/001-service-decomposition.md` |
| 4 | `characterization/AlbumCrudCharacterizationTest.java` + 4 siblings |
| 5 | `catalog-service/` module (new), `AlbumController.java` (strangler proxy) |
| 6 | `catalog-service/acl/AlbumTranslator.java`, `fence/BoundaryLeakTest.java`, `.claude/hooks/fence-check.sh`, `docs/adr/002-fence-strategy.md` |
| 7 | `eval/golden_set.json`, `eval/run_eval.py`, `eval/ci_gate.sh` |
| 8 | `docs/runbook/cutover.md` |
| 9 | `.claude/commands/score-seam.md`, `docs/scout-report.md` |
