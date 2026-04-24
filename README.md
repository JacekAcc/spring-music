# Spring Music — Modernisation Workshop

A Spring Boot monolith being safely decomposed into microservices using Claude Code as the engineering partner. The goal is not speed — it is **zero behaviour regressions** throughout the extraction.

**Current status:** Phase 1 complete — context hierarchy, slash commands, and agent pipeline established. Phases 2–6 in progress.

---

## The Problem

Extracting services from a monolith is the riskiest refactoring category. The usual failure modes are:

- Starting the cut before pinning current behaviour → regressions that nobody notices until production
- Letting the new service leak the old data model → you extracted the code but not the coupling
- No rollback plan → the 3am cutover becomes a 9am incident

This repo works through all three systematically, using Claude Code to do the archaeology and enforce the boundaries.

## The Approach

**Strangler fig with test-pinned extraction.** Each extraction follows the same sequence:

```
pin behaviour → rank seams → extract → fence boundary → write runbook → score quality
```

Claude Code coordinates the work using a 6-phase plan with explicit gates. No phase starts until the previous phase's gate condition is verified by running tests.

## How Claude Was Taught to Work Here

Three distinct layers of context keep Claude from making the wrong call:

| Layer | File | What it enforces |
|-------|------|-----------------|
| User | `~/.claude/CLAUDE.md` | English, conventional commits, terse style |
| Project | `CLAUDE.md` _(this file)_ | Service boundary rules, agent order, test tag conventions |
| Directory | `src/.../music/CLAUDE.md` | "You are in the monolith root — prefer extraction over addition" |
| Directory | `new-service/CLAUDE.md` | "Never reference monolith packages; these field names are forbidden in the API" |

**Why directory-level CLAUDE.md?** Because a project-level instruction like "don't add to the monolith" is easy to ignore. A file that Claude reads the moment it opens a file inside `music/` is impossible to miss. The instruction is co-located with the risk.

**Hooks vs. prompts for enforcement.** The PreToolUse fence hook (Phase 5) blocks any edit that writes monolith-internal field names into `catalog-service/`. This is a hook, not a prompt instruction, because enforcement that can be "reasoned around" is not enforcement. The hook exits 1; the edit never happens. See `docs/adr/002-fence-strategy.md` for the decision record.

**Parallel agent spawning.** Three of the six phases spawn two agents simultaneously in a single coordinator message. Agents receive full explicit context in their prompts — they do not inherit the coordinator's conversation. This is the explicit-context pattern required when subagents must be independently reproducible.

## Phases and Progress

| Phase | What | Agents | Gate | Status |
|-------|------|--------|------|--------|
| 1 | Context hierarchy + commands | _(manual)_ | CLAUDE.md levels in place | **Done** |
| 2 | Stories + decomposition ADR | `pm-stories` \|\| `architect-map` | ADR with seam ranking table | Pending |
| 3 | Characterization tests + scout report | `tester-pin` \|\| `agentic-scouts` | `./gradlew test --tests "*.characterization.*"` green | Pending |
| 4 | Extract catalog service | `dev-cut` | Both `@Tag("characterization")` and `@Tag("contract")` green | Pending |
| 5 | Fence + cutover runbook | `dev-fence` \|\| `ops-weekend` | `BoundaryLeakTest` passing; hook registered | Pending |
| 6 | Eval harness | `quality-scorecard` | False-confidence rate < 20% in CI | Pending |

## Quick Start

```bash
# Build
./gradlew clean assemble

# Run (H2 in-memory, no setup needed)
java -jar build/libs/spring-music.jar

# Tests
./gradlew test
./gradlew test --tests "*.characterization.*"   # pin suite only (Phase 3+)
./gradlew test --tests "*.fence.*"              # boundary leak test (Phase 5+)

# Eval scorecard (Phase 6+)
python eval/run_eval.py
```

## Custom Slash Commands

Reusable playbooks registered in `.claude/commands/`:

| Command | What it does |
|---------|-------------|
| `/extract-service <seam>` | 7-step extraction playbook: verify pins → locate seam → create module → add strangler proxy → add ACL → verify both suites green |
| `/pin-behavior` | Runs characterization suite, reports deviations with pass/fail table, recommends whether each failure is a regression or an intentional change |
| `/score-seam <seam>` | Gathers explicit context for a seam, spawns a Task subagent with scoring rubric, compares verdict against human ADR ranking |

## Repository Layout

```
spring-music/
  src/                          # monolith source
    main/java/.../music/
      CLAUDE.md                 # directory-level: monolith context
  new-service/
    CLAUDE.md                   # directory-level: extracted service context
  catalog-service/              # extracted service (Phase 4+)
  docs/
    stories.md                  # user stories (Phase 2)
    adr/                        # architecture decision records
      001-service-decomposition.md
      002-fence-strategy.md
    runbook/cutover.md          # ops 3am runbook (Phase 5)
    scout-report.md             # per-seam risk scores (Phase 3)
  eval/                         # LLM refactoring eval harness (Phase 6)
  .claude/
    agents/                     # 8 specialised subagents
    commands/                   # 3 slash command playbooks
    hooks/fence-check.sh        # PreToolUse hook (Phase 5)
    settings.json               # hook registration
  PLAN.md                       # 6-phase coordinator plan with dependency graph
```

---

## Original Spring Music Application

<details>
<summary>Cloud Foundry setup and deployment instructions</summary>

This application demonstrates swappable persistence backends on Cloud Foundry using Spring Data and Spring profiles. The same REST API works against H2, MySQL, PostgreSQL, MongoDB, or Redis — selected at startup via a profile flag.

### Building

```bash
./gradlew clean assemble
# Output: build/libs/spring-music-1.0.jar
```

Requires Java 8+.

### Running locally

```bash
# H2 in-memory (no setup)
java -jar build/libs/spring-music.jar

# External database
java -jar -Dspring.profiles.active=mysql build/libs/spring-music.jar
# Profiles: mysql | postgres | mongodb | redis
```

### Cloud Foundry deployment

```bash
cf push
```

`manifest.yml` points to `build/libs/spring-music-1.0.jar` (1 GB memory, random route).

### Creating and binding services

```bash
cf marketplace
cf create-service <service> <plan> <name>
cf bind-service spring-music <name>
cf restart spring-music
```

User-provided service URI format: `<dbtype>://<user>:<pass>@<host>:<port>/<db>`

### Database drivers

MySQL, Postgres, MSSQL, MongoDB, and Redis drivers are included. For Oracle, download `ojdbc7.jar` or `ojdbc8.jar`, place in `libs/`, and uncomment the relevant line in `build.gradle`.

</details>
