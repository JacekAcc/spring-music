# Team <name>

## Participants
- Name (role(s) played today)
- Name (role(s) played today)
- lukasz-acn (frontend developer)

## Scenario
Scenario 1: Spring Music — Modernisation Workshop

## What We Built
Replaced the original AngularJS 1.2.16 SPA (2013, EOL) with a modern Vue 3 + Vite + Tailwind CSS
frontend living in `frontend/`. The new app covers all original features: album CRUD, grid/list view
toggle, client-side sorting, inline field editing, add/edit modal with year validation, success/error
notifications, active profile badges in the header, and an error-testing page.

The Spring Boot REST API (`/albums`, `/appinfo`, `/errors`) is untouched. The frontend project is
self-contained — `npm run build` produces a `dist/` folder ready to be served by Spring Boot once
the Gradle wiring is added.

## Challenges Attempted
| # | Challenge | Status | Notes |
|---|---|---|---|
| 1 | The <name> | done / partial / skipped | |
| 2 | | | |

## Key Decisions
- **Vue 3 over React / Angular** — less boilerplate than React for a CRUD app this size; more approachable migration path from AngularJS than Angular 17+.
- **Tailwind CSS over Bootstrap** — utility-first styling avoids the jQuery dependency and the outdated Bootstrap 3 look; no custom CSS file needed.
- **Plain JavaScript over TypeScript** — keeps the scaffolding lightweight for a demo app; TypeScript can be layered on later.
- **Decoupled build (dist/ not wired into Gradle yet)** — avoids requiring a local Java install during frontend development. When Java is available, `vite.config.js` `build.outDir` can point at `src/main/resources/static/` and a Gradle exec task can drive the npm build.

## How to Run It
```bash
# Install dependencies (once)
cd frontend
npm install

# Development server (proxies /albums, /appinfo, /errors → localhost:8080)
npm run dev
# → open http://localhost:5173

# Production build (outputs to frontend/dist/)
npm run build
```

Requires Node.js 18+. For the API calls to work, Spring Boot must be running on port 8080 (needs Java / Docker).

## If We Had More Time
- Wire the Vite build into Gradle so `./gradlew build` produces a self-contained JAR with the Vue app inside.
- Add frontend tests (Vitest for unit, Playwright for e2e).
- Replace the `confirm()` delete dialog with a proper inline confirmation component.
- Add an album cover placeholder or colour-coded genre badge to the grid cards.

## How We Used Claude Code
Claude explored the existing AngularJS codebase, proposed the migration strategy (framework choice, build integration, component mapping), and scaffolded the entire `frontend/` directory — all 19 files — in a single session. The planning phase (ExitPlanMode) caught the Java/Gradle constraint before any code was written, which saved a full rework. The biggest time saving was the 1-to-1 mapping of AngularJS controllers/directives to Vue components: what would have taken hours of reading AngularJS docs was done in minutes.
