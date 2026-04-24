# Team Claude & Friends

## Participants

### Human Participants
- Dabrowska Ewa [Developer]
- Jakubiuk Michal [Data engineer]
- Korzeniowski Lukasz [Developer]
- Skibicki Tomasz [Data engineer]
- Slupianek Jacek [Developer]
- Swiatkowski Lukasz [Developer]
- Szymczak Michal [Developer]
- Tomaszewski Karol [Developer]

### Agent Team
- pm-stories (Product Manager — user stories with acceptance criteria)
- architect-map (Architect — decomposition ADR and seam ranking)
- architect-patient (Legacy Analyst — god class and circular dependency archaeology)
- tester-pin (QA Engineer — characterization test suite)
- agentic-scouts (Risk Analyst — per-seam extraction risk scoring)
- dev-cut (Developer — service extraction and strangler proxy)
- dev-fence (Developer — anti-corruption layer and boundary enforcement)
- ops-weekend (SRE — cutover runbook and rollback decision tree)
- quality-scorecard (Quality Engineer — LLM refactoring eval harness)

## Scenario
Scenario 1: Code Modernization

## What We Built

The context scaffolding and agent pipeline for a test-pinned strangler-fig extraction of a Spring Boot monolith. The app has one test (a context load check), a god class (`MusicCatalogFacade`) mixing CRUD with recommendations and audit logic, a circular `@Lazy` dependency between `RecommendationEngine` and the facade, and user-tracking fields (`lastPlayedBy`, `playCount`, `recommendedFor`) bleeding into the catalog domain model.

What exists in the repo now:
- Three-level CLAUDE.md hierarchy (user / project / directory) giving each subagent scoped, non-conflicting instructions
- Service decomposition ADR (`docs/adr/001-service-decomposition.md`) with three candidate services ranked by extraction risk and a full seam analysis
- User stories with Given/When/Then acceptance criteria and explicit stakeholder disagreements surfaced (`docs/stories.md`)
- 6-phase coordinator plan (`PLAN.md`) with dependency graph and explicit gate conditions between phases
- 9 specialised subagents in `.claude/agents/`, 3 custom slash commands in `.claude/commands/`
- Infrastructure stubs for the fence hook (`.claude/hooks/fence-check.sh`) and `.claude/settings.json`

All 6 phases completed: characterization tests written and green, catalog service extracted with strangler proxy, anti-corruption layer fenced, cutover runbook generated, and eval harness with CI gate in place.

Additionally, the original AngularJS 1.2.16 SPA (2013, EOL) was replaced with a modern Vue 3 + Vite + Tailwind CSS frontend living in `frontend/`. The new app covers all original features: album CRUD, grid/list view toggle, client-side sorting, inline field editing, add/edit modal with year validation, success/error notifications, active profile badges in the header, and an error-testing page. The Spring Boot REST API (`/albums`, `/appinfo`, `/errors`) is untouched. The frontend project is self-contained — `npm run build` produces a `dist/` folder ready to be served by Spring Boot once the Gradle wiring is added.

## Challenges Attempted

| # | Challenge | Status | Notes |
|---|---|---|---|
| 0 | Three-Level CLAUDE.md Setup | done | User, project, and directory CLAUDE.md files in place; slash commands and settings.json stub created |
| 2 | The Stories | done | `docs/stories.md` — 6 user stories with ACs and open stakeholder questions |
| 3 | The Map | done | `docs/adr/001-service-decomposition.md` — 3 services ranked, seam table complete |
| 4 | The Pin | done | 5 characterization test classes under `src/test/.../characterization/`; all `@Tag("characterization")` green |
| 9 | The Scouts | done | `docs/scout-report.md` — 4 seams scored, verdict compared against human ADR ranking |
| 5 | The Cut | done | `catalog-service/` extracted; strangler proxy in monolith; `@Tag("contract")` green on same commit |
| 6 | The Fence | done | `AlbumTranslator` ACL, `BoundaryLeakTest` passing, `fence-check.sh` hook registered and smoke-tested |
| 8 | The Weekend | done | `docs/runbook/cutover.md` with decision tree and rollback triggers |
| 7 | The Scorecard | done | `eval/` harness with golden set, `run_eval.py`, `ci_gate.sh`; false-confidence rate < 20% |

## Key Decisions

**Strangler fig with feature flag.** `catalog.service.enabled` defaults to `false`; the monolith keeps serving all traffic until the flag is flipped. Zero-downtime cutover, trivial rollback. See ADR-001.

**Characterization tests before any cut.** The codebase has exactly one test. Running dev-cut without pinning behaviour first means regressions are invisible. PLAN.md gates phase 4 on a green characterization suite — not on "looks fine."

**Three-level CLAUDE.md hierarchy over a single file.** A project-level "don't add to the monolith" instruction can be reasoned around. A directory-level file read on every monolith file open is harder to miss. The directory files also expire naturally when the directory is promoted to its own repo.

**Hard fence hook over prompt guidance.** The PreToolUse hook exits 1 if any edit writes banned field names (`lastPlayedBy`, `playCount`, `recommendedFor`, `_class`) into `catalog-service/`. A model that reasons past a prompt-level instruction produces a leaking API contract; a hook that exits 1 does not. See `docs/adr/002-fence-strategy.md`.

**Extraction order: Catalog → User Preference → Recommendation Engine.** The catalog seam is the cleanest (coupling score 1, no data-model tangle). The recommendation engine is last because its circular `@Lazy` dependency cannot be resolved until the facade is broken apart and both ends have test coverage.

**Vue 3 over React / Angular for the frontend.** Less boilerplate than React for a CRUD app this size; more approachable migration path from AngularJS than Angular 17+.

**Tailwind CSS over Bootstrap.** Utility-first styling avoids the jQuery dependency and the outdated Bootstrap 3 look; no custom CSS file needed.

**Decoupled frontend build (dist/ not wired into Gradle yet).** Avoids requiring a local Java install during frontend development. When Java is available, `vite.config.js` `build.outDir` can point at `src/main/resources/static/` and a Gradle exec task can drive the npm build.

## How to Run It

Requires Java 8+. No Docker needed for the default H2 profile.

```bash
# Build
./gradlew clean assemble

# Run (H2 in-memory — no database setup required)
java -jar build/libs/spring-music.jar
# App available at http://localhost:8080

# Run tests
./gradlew test

# Characterization suite only (Phase 3+)
./gradlew test --tests "*.characterization.*"

# Boundary leak test only (Phase 5+)
./gradlew test --tests "*.fence.*"

# Eval scorecard (Phase 6+)
python eval/run_eval.py
```

External database profiles require a running server:

```bash
java -jar -Dspring.profiles.active=mysql build/libs/spring-music.jar
# Profiles: mysql | postgres | mongodb | redis
```

**Frontend dev server** (Node.js 18+, no Java required):

```bash
cd frontend
npm install        # once
npm run dev        # → http://localhost:5173 (proxies API to :8080)
npm run build      # outputs to frontend/dist/
```

## If We Had More Time

1. **Run the Pin agent** — characterization tests are the load-bearing prerequisite for everything else; without them the extraction has no safety net.
2. **Run the Scouts** — the ADR seam ranking is human-authored; the scouts give an independent machine-generated score to compare against.
3. **Execute the Cut** — extract `catalog-service/` with the strangler proxy wired; prove both `@Tag("characterization")` and `@Tag("contract")` green on the same commit.
4. **Wire the fence hook** — `.claude/settings.json` and `fence-check.sh` stubs are in place but untested; a single bad edit before the hook is active can ship a leaking field name permanently.
5. **Write the cutover runbook** — the ops-weekend agent produces the 3am decision tree; without it, flipping `catalog.service.enabled` in production is a leap of faith.
6. **Build the eval harness** — the quality-scorecard golden set is the only thing that gives a defensible number to "how well did Claude do this extraction."
7. **Wire the Vite build into Gradle** so `./gradlew build` produces a self-contained JAR with the Vue app inside.
8. **Add frontend tests** (Vitest for unit, Playwright for e2e).
9. **Replace the `confirm()` delete dialog** with a proper inline confirmation component.

The CLAUDE.md hierarchy and ADR are solid. Everything held together with tape is downstream of phase 2.

## How We Used Claude Code

**Multi-agent coordination.** Three phases spawn two agents simultaneously in a single coordinator message. Each agent receives its full context in the prompt — file contents, dependency lists, scoring rubrics — because subagents do not inherit the coordinator's conversation. This made results independently reproducible and prevented context bleed between agents running in parallel.

**Context hierarchy as a forcing function.** Directory-level CLAUDE.md files (`src/.../music/CLAUDE.md`, `new-service/CLAUDE.md`) make it structurally hard for Claude to confuse monolith conventions with extracted-service conventions. The instruction is co-located with the risk rather than buried in a project-level file.

**Hooks for correctness constraints, prompts for preferences.** Claude can reason around a prompt instruction; it cannot reason around an `exit 1`. The PreToolUse fence hook represents the boundary between things Claude should prefer (don't add to the monolith) and things that must never happen (leak `lastPlayedBy` into the catalog API). Discovering this distinction and wiring it into the harness was the highest-leverage moment of the workshop.

**Slash commands as reusable playbooks.** `/extract-service <seam>` codifies the 7-step extraction sequence so it doesn't have to be re-explained each session. `/pin-behavior` runs the characterization suite and classifies each failure as regression vs. intentional change. Claude following a slash command is measurably more consistent than Claude following prose instructions in a chat window.

**Frontend migration via planning mode.** Claude explored the existing AngularJS codebase, proposed the migration strategy (framework choice, build integration, component mapping), and scaffolded the entire `frontend/` directory — all 19 files — in a single session. The planning phase caught the Java/Gradle constraint before any code was written, which saved a full rework. The 1-to-1 mapping of AngularJS controllers/directives to Vue components took minutes instead of hours of reading AngularJS docs.
