# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## How Claude Is Taught to Work Here

This project uses three layers of context so Claude makes the right call without being reminded every session:

| Layer | File | Enforces |
|-------|------|----------|
| User | `~/.claude/CLAUDE.md` | English, conventional commits, terse style — all projects |
| Project | this file | Agent order, service boundary rules, test tag conventions |
| Directory | `src/.../music/CLAUDE.md` | "You are in the monolith root — prefer extraction over addition" |
| Directory | `new-service/CLAUDE.md` | "Never reference monolith packages; these field names are banned from the API" |

**Hooks vs. prompts:** Boundary enforcement that can be "reasoned around" is not enforcement. The PreToolUse hook in `.claude/hooks/fence-check.sh` (active from Phase 5) exits 1 if any Edit or Write would put a monolith-internal field name into `catalog-service/`. That is a hard block. Directory-level CLAUDE.md instructions are preference guidance — they have legitimate exceptions (e.g., a bug fix in the monolith while extraction is in progress). See `docs/adr/002-fence-strategy.md`.

**Parallel agents with explicit context:** Three of the six phases spawn two subagents simultaneously. Each agent receives its complete context in the prompt — file contents, dependency lists, scoring rubrics. Subagents do not inherit the coordinator's conversation. This makes results independently reproducible and prevents context bleed between concerns.

## Build and Run

```bash
# Build runnable JAR
./gradlew clean assemble
# Output: build/libs/spring-music-1.0.jar

# Full build including tests
./gradlew build

# Run tests only
./gradlew test

# Run locally (defaults to in-memory H2)
java -jar build/libs/spring-music.jar

# Run with a specific database profile
java -jar -Dspring.profiles.active=mysql build/libs/spring-music.jar
# Profiles: mysql | postgres | mongodb | redis
```

## Architecture

Spring Boot app that demonstrates swappable persistence backends using Spring Data and Spring profiles. The same REST API and domain model work against H2, MySQL, PostgreSQL, MongoDB, or Redis — selected at startup via a single profile flag.

**Key source packages** under `src/main/java/org/cloudfoundry/samples/music/`:

- `web/AlbumController.java` — REST CRUD endpoints for albums (`/albums`)
- `domain/Album.java` — single domain entity; JPA-annotated but reused across all backends
- `repositories/` — three Spring Data implementations:
  - `jpa/JpaAlbumRepository` — active when neither `mongodb` nor `redis` profile is set
  - `mongodb/MongoAlbumRepository` — active on `mongodb` profile
  - `redis/RedisAlbumRepository` — active on `redis` profile (custom `CrudRepository` impl)
- `config/SpringApplicationContextInitializer.java` — detects active profile, validates only one DB type is bound, and excludes conflicting auto-configurations
- `repositories/AlbumRepositoryPopulator.java` — seeds the database from `albums.json` on `ApplicationReadyEvent` if the repository is empty

All three repository implementations inject as `CrudRepository<Album, String>`, so `AlbumController` is completely decoupled from the backend.

## Database Profiles

| Profile | Backend | Local prerequisite |
|---------|---------|-------------------|
| _(none)_ | H2 in-memory | none |
| `mysql` | MySQL | `music` database on localhost |
| `postgres` | PostgreSQL | `music` database on localhost |
| `mongodb` | MongoDB | MongoDB on localhost |
| `redis` | Redis | Redis on localhost |

Connection defaults are in `src/main/resources/application.yml`. On Cloud Foundry, `SpringApplicationContextInitializer` auto-detects bound service tags via Java-cfenv and enables the correct profile automatically.

## Frontend

AngularJS 1.x SPA served from `src/main/resources/static/`. It communicates with the REST API and has no build step — static assets are bundled into the JAR by Spring Boot.

## Testing

Only `ApplicationTests.java` exists — a single Spring context load test. Run it with `./gradlew test`. There are no database-specific integration tests.

## Cloud Foundry Deployment

```bash
cf push
```

`manifest.yml` points to `build/libs/spring-music-1.0.jar` (1 GB memory, random route). Spring auto-reconfiguration is disabled; Java-cfenv handles service binding detection instead.

## Modernisation Agents (in suggested order)

Subagents for the legacy modernisation workshop. Invoke with `@<agent-name>` in Claude Code.

| Agent | Challenge | When to invoke |
|-------|-----------|----------------|
| `pm-stories` | The Stories | First — write user stories with testable ACs before touching code |
| `architect-map` | The Map | After Stories; produces the decomposition ADR with seams ranked by extraction risk |
| `tester-pin` | The Pin | After Map, before any refactoring; writes characterization tests that pin current behaviour |
| `agentic-scouts` | The Scouts | After Map; fan-out risk analysis — one subagent per candidate seam, aggregated into a ranked list |
| `dev-cut` | The Cut | After Pin; extracts the first service — both monolith and new service must stay green |
| `dev-fence` | The Fence | After Cut; builds the anti-corruption layer and enforces the package boundary |
| `ops-weekend` | The Weekend | After Cut; generates the 3am runbook with decision tree and rollback triggers |
| `quality-scorecard` | The Scorecard | After Fence; eval harness with golden set and CI-runnable metrics for LLM-driven refactoring |

Agents are defined in `.claude/agents/`. Each agent file is self-contained — read it for detailed instructions on what it produces and what it expects as input from prior agents.

## Workshop Conventions

**Execution plan:** See `PLAN.md` for the 6-phase coordinator plan with dependency graph and gate conditions.

**Never add to the monolith** what belongs in an extracted service. If in doubt, check the seam ranking in `docs/adr/001-service-decomposition.md`.

**Test tags:**
- `@Tag("characterization")` — pins current monolith behaviour; must never be deleted, only updated with explicit intent
- `@Tag("contract")` — verifies the extracted service's API shape
- Both suites must be green on every commit that touches extraction code

**Custom slash commands** (`.claude/commands/`):
- `/extract-service <seam-name>` — step-by-step extraction playbook
- `/pin-behavior` — runs characterization suite and reports deviations
- `/score-seam <seam-name>` — invokes Scouts subagent for a named class/package

## Service Boundary Rules

1. The monolith (`org.cloudfoundry.samples.music.*`) must never import from the extracted service (`org.cloudfoundry.catalog.*`).
2. Cross-boundary calls go only through the strangler proxy in `web/AlbumController` via `RestTemplate`.
3. The extracted service's public API must never expose these monolith-internal field names: `lastPlayedBy`, `playCount`, `recommendedFor`, `_class`.
4. All data crossing the boundary must pass through `acl/AlbumTranslator` — no direct field mapping elsewhere.
5. The `catalog.service.enabled` flag controls the strangler proxy; default is `false` (monolith serves all traffic).
