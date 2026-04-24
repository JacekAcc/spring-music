# Spring Music — User Stories

**Version:** 1.0
**Date:** 2026-04-24
**Author:** pm-stories agent
**Codebase root:** `c:\EDF\claude-code-workshop\spring-music`

---

## Story S1 — Browse Catalog

**As a** catalogue visitor
**I want** to view all albums in the catalogue, switch between grid and list layouts, and sort by title, artist, release year, or genre
**So that** I can find albums I am interested in without knowing their exact names in advance

### Acceptance Criteria

- [ ] AC1: `GET /albums` returns HTTP 200 and a JSON array containing all seeded albums; with the default H2 profile and a fresh start, the response contains exactly 29 albums matching `src/main/resources/albums.json`.
- [ ] AC2: The AngularJS frontend renders all returned albums in grid view by default on page load, with each card showing at minimum `title`, `artist`, `releaseYear`, and `genre`.
- [ ] AC3: Clicking the list-view icon switches the display to `templates/list.html`; clicking the grid icon switches back — no page reload occurs.
- [ ] AC4: Clicking "title", "artist", "year", or "genre" sort links re-orders the displayed albums client-side in ascending order by that field; clicking the chevron icon reverses the sort direction.
- [ ] AC5: If the backend returns HTTP 500 or is unreachable, the frontend displays an error-class alert (red `alert-danger` banner) rather than a blank or broken page.
- [ ] AC6: Albums whose `trackCount` is 0 (seed data omits the field) still appear in the list without rendering a broken or `null` value for track count.
- [ ] AC7 (H2 profile only): After application restart, the full 29-album seed set reappears — any albums added during the previous session are lost. This must be called out in user-facing documentation or a UI notice.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Catalogue Manager | High | Core discovery feature; without it users cannot interact with anything else |
| Platform Engineer | Medium | Browsing is a read-only `findAll()` with no pagination or filtering at the API level; the entire catalogue is dumped on every call regardless of size, making this a latency risk that the ops team would need to mitigate before it is truly "done" |

**Unresolved disagreement — search vs. filter-only:** The catalogue team wants free-text search across `title` and `artist`. The platform team considers that out of scope for an in-process `CrudRepository` and would only support client-side filtering of the already-loaded array. Neither position is adopted here; both must be evaluated before the browse story is called complete.

**Unresolved disagreement — pagination size:** The catalogue team is comfortable with a single-page display of the full catalogue (currently 29 albums). The platform team flags that `GET /albums` has no server-side pagination and will return unbounded results as the catalogue grows, requiring a `Pageable` change to the repository interface. No default page size has been agreed.

### Out of Scope (this story)
- Server-side search or filtering query parameters
- Pagination controls in the UI
- Album artwork or cover images
- User authentication or personalised views

### Testing Notes
- Run against H2 (no profile flag) for the baseline 29-album count. Any other profile requires a pre-seeded database.
- The seed occurs in `AlbumRepositoryPopulator` only when the repository is empty; manually pre-inserting records will suppress seeding.
- Client-side sort is pure AngularJS `orderBy` — no API call is made. Verify by watching network traffic.

---

## Story S2 — Add / Update Album

**As a** catalogue manager
**I want** to add a new album or edit an existing album's `title`, `artist`, `releaseYear`, `genre`, or `trackCount`
**So that** the catalogue reflects accurate, up-to-date information about the music library

### Acceptance Criteria

- [ ] AC1: Clicking "add an album" opens a modal form with required fields `title`, `artist`, `releaseYear`, and `genre`; the OK button is disabled until all four fields are non-empty.
- [ ] AC2: `releaseYear` is validated against the pattern `/^[1-2]\d{3}$/` in the frontend before submission; entering "abcd" or "99" keeps the OK button disabled and shows a warning glyph.
- [ ] AC3: Submitting a valid add form issues `PUT /albums` with a JSON body containing at minimum `title`, `artist`, `releaseYear`, and `genre`; the server returns HTTP 200 and the saved album including its server-generated `id`.
- [ ] AC4: After a successful add, the album list refreshes automatically and a green "Album saved" success banner appears.
- [ ] AC5: Clicking an existing album's in-place edit control for any field (`title`, `artist`, `releaseYear`, `genre`), editing the value, and pressing Enter or the checkmark issues `POST /albums` and updates that album in the list.
- [ ] AC6: In-place save with an empty string is blocked client-side; the save function returns `false` without issuing an API call.
- [ ] AC7: If the server returns a non-2xx status on save, a red "Error saving album: {status}" banner appears and the list is not refreshed.
- [ ] AC8: Submitting an album with a `releaseYear` that passes the client-side pattern but is not a plausible year (e.g., "2999") is accepted by the current server — this is a known gap; the AC records the current behaviour, not the desired future state.
- [ ] AC9 (H2 profile): An album added during a session is lost after application restart. The UI provides no warning of this. The AC passes if the add succeeds within the session; the persistence gap is flagged separately in S6.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Catalogue Manager | High | Without write access the catalogue is read-only; adding new releases is a routine daily task |
| Platform Engineer | Medium | The write path works but has no authentication, no input sanitisation beyond the four required fields, and no duplicate detection — shipping it to production without those controls is a risk the ops team would block |

**Unresolved disagreement — who can edit:** The catalogue team assumes any user who can load the page can add or update albums. There is no authentication or role check anywhere in `AlbumController` or the AngularJS app. A business stakeholder representing data integrity would require at minimum an admin role before the edit controls are shown. This disagreement is not resolved here; both positions must be brought to the product owner.

### Out of Scope (this story)
- `trackCount` is not present in the modal form (`albumForm.html`) — adding it to the form is a separate story
- `albumId` field (external catalogue reference) — not surfaced in the UI
- Duplicate detection (same `title` + `artist` combination)
- Bulk import

### Testing Notes
- Test the in-place editor by clicking directly on a field value in the list or grid view, not the modal.
- The add modal uses `PUT`, the in-place save uses `POST` — verify both HTTP methods in network traffic.
- On the MongoDB profile, `id` is stored as `_id` internally but the API returns it as `id`; field mapping is transparent to this story.

---

## Story S3 — Delete Album

**As a** catalogue manager
**I want** to remove an album from the catalogue permanently
**So that** out-of-date, duplicate, or incorrectly entered albums do not appear to users

### Acceptance Criteria

- [ ] AC1: Each album in the list or grid view has a delete control; clicking it issues `DELETE /albums/{id}` where `{id}` is the album's server-assigned identifier.
- [ ] AC2: On HTTP 200 from the delete endpoint, a green "Album deleted" banner appears and the album is removed from the displayed list without a full page reload.
- [ ] AC3: On HTTP 4xx or 5xx, a red "Error deleting album: {status}" banner appears and the album remains in the displayed list.
- [ ] AC4: Attempting to delete an album with an `id` that does not exist (e.g., `DELETE /albums/nonexistent-id`) returns HTTP 200 (current behaviour of `CrudRepository.deleteById` — it does not throw on missing id); the AC records this as current observable behaviour.
- [ ] AC5: After deletion, a subsequent `GET /albums` does not include the deleted album.
- [ ] AC6 (H2 profile): Deleting an album during a session and then restarting the application causes the full seed set to re-appear, including the deleted album. This is expected H2 behaviour; the AC passes if deletion works within the session.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Catalogue Manager | High | Correcting bad data requires deletion; without it mistakes are permanent |
| Platform Engineer | Low | Hard delete with no audit trail, no soft-delete flag, and no confirmation prompt is an operational risk — accidental deletion is unrecoverable on persistent backends |

**Unresolved disagreement — soft delete vs. hard delete:** The catalogue team is satisfied with hard delete (current implementation). The platform/compliance team would require a soft-delete pattern (e.g., a `deleted` boolean flag, keeping the row and filtering it from `GET /albums`) to support audit and recovery. The current implementation performs an immediate, irreversible `repository.deleteById(id)`. This disagreement must be resolved before the story can be marked production-ready.

**Unresolved disagreement — audit trail requirement:** The compliance stakeholder may require a record of who deleted what and when. No audit fields exist in the `Album` domain entity, and no `AuditService` is wired into the delete path. The platform team would flag this as a blocker for regulated environments.

### Out of Scope (this story)
- Confirmation dialog before deletion (noted as a UX gap but not in scope here)
- Bulk delete
- Undo / restore functionality
- Audit log storage

### Testing Notes
- There is no confirmation prompt in the current implementation — delete fires immediately on click. Testers should not expect one.
- Verify deletion is durable between browser refreshes (not just a client-side list update) by calling `GET /albums/{id}` after delete.
- On Redis profile, key expiry could cause an album to disappear independently of delete — ensure the test environment has no TTL set on album keys.

---

## Story S4 — View Recommendations

**As a** catalogue visitor
**I want** to see a list of albums recommended based on what I am currently viewing or have recently browsed
**So that** I can discover related music without having to search manually

> **Implementation status:** This capability does not exist in the codebase. No recommendation endpoint, service, or data model is present. This story describes what should be built.

### Acceptance Criteria

- [ ] AC1: A `GET /albums/{id}/recommendations` endpoint returns a JSON array of up to 5 `Album` objects that share the same `genre` as the requested album, excluding the requested album itself.
- [ ] AC2: If the requested album `id` does not exist, the endpoint returns HTTP 404 with a machine-readable error body.
- [ ] AC3: If fewer than 5 albums share the same `genre`, the response contains however many exist (including zero); it does not pad with albums from other genres.
- [ ] AC4: The recommendations array is displayed in the UI below or alongside the album detail view without requiring a page reload.
- [ ] AC5: If the recommendations endpoint returns HTTP 500 or is unreachable, the album detail view still loads; a non-blocking notice ("Recommendations unavailable") is shown rather than an error page.
- [ ] AC6 (cold start): If a visitor has no browsing history and is viewing an album for the first time, the genre-based fallback applies — recommendations are based solely on `genre`, not on any user history.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Catalogue Manager | High | Discovery is a key retention driver; users who find related albums are more likely to return |
| Platform Engineer | Low | New capability requiring a new endpoint, potential new data store for history, and cold-start logic — high implementation cost relative to current MVP scope |

**Unresolved disagreement — personalised vs. genre-based:** The catalogue team wants personalised recommendations based on individual listening or browsing history. The platform team points out there is no user identity model, no session tracking, and no history store — personalisation would require building at least three new capabilities before recommendations could be personalised. Genre-based recommendations are the minimum viable fallback but the catalogue team considers them insufficient for a useful feature.

**Unresolved disagreement — cold-start handling:** If personalisation is eventually built, a new user (or anonymous visitor) has no history. The catalogue team wants a "popular albums" fallback (requires a play count — see S5). The platform team prefers random-within-genre to avoid building a separate popularity ranking system. Neither approach is adopted here.

### Out of Scope (this story)
- Machine learning or collaborative filtering
- "Users who liked this also liked" cross-user recommendations
- Recommendations based on `artist` similarity (only `genre` is in scope for AC1)
- Any persistent storage of recommendation clicks

### Testing Notes
- This endpoint does not exist; testers should treat this story as specification until implementation begins.
- The genre-based query requires the active repository to support filtering by `genre`. `CrudRepository` does not provide this out of the box — a derived query method or custom query will be needed in the JPA, MongoDB, and Redis repository implementations.
- On H2, test that a genre with only one album returns an empty recommendations array for that album.

---

## Story S5 — Play History

**As a** catalogue visitor
**I want** to see which albums I have previously viewed or played
**So that** I can return to albums I was interested in without having to search again

> **Implementation status:** This capability does not exist in the codebase. No history endpoint, user model, or event store exists.

### Acceptance Criteria

- [ ] AC1: A `GET /history` endpoint returns a time-ordered JSON array of album interactions for the current session or user, each entry containing at minimum the album `id`, `title`, `artist`, and an ISO-8601 timestamp of the interaction.
- [ ] AC2: Viewing an album detail page or triggering a "play" action records an entry in the history store and makes it retrievable via `GET /history` within the same session.
- [ ] AC3: If no history exists for the current user or session, `GET /history` returns HTTP 200 with an empty array — not HTTP 404.
- [ ] AC4: History entries reference albums by `id`; if an album is subsequently deleted, the history entry remains (the deletion does not cascade to history).
- [ ] AC5: The `GET /history` endpoint returns HTTP 400 if a caller provides a `userId` query parameter referencing a user that does not belong to the authenticated session (prevents one user reading another's history).
- [ ] AC6: History entries older than the agreed retention period (see Priority Disagreements) are automatically purged and are not returned by `GET /history`.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Catalogue Manager | Medium | Useful for returning users, but not a blocker for basic catalogue management |
| Platform Engineer | Low | Requires user identity, session management, an append-only event store, and a retention/purge job — none of which exist; cost is high relative to current scope |

**Unresolved disagreement — per-user vs. global history:** The catalogue team wants per-user play history so personalised recommendations (S4) can be built on top of it. The platform team notes that there is no authentication model and no user identity in the system — implementing per-user history requires building identity first. A global (anonymous, session-scoped) history is technically simpler but provides no personalisation value. This must be resolved before implementation begins.

**Unresolved disagreement — GDPR retention limit:** The legal/compliance stakeholder requires that personal listening history be deleted after a defined retention period (30 days is a common default). The catalogue team wants indefinite history for better recommendations. The platform team would implement a configurable TTL but needs a policy decision from legal before coding begins. The value of AC6's retention period is intentionally left blank pending that decision.

### Out of Scope (this story)
- Play count aggregation across all users (global popularity ranking)
- Integration with external music streaming services
- Export of history data by the user (GDPR subject access request — separate story)
- `playCount` or `lastPlayedBy` fields on the `Album` entity — these belong in the history store, not the album record

### Testing Notes
- This endpoint does not exist; testers should treat this story as specification until implementation begins.
- Any implementation must document the retention period in the API contract before this story can be signed off.
- On H2 profile, session-scoped history stored in memory is lost on restart — acceptable for development but must be flagged if a non-persistent backend is used in a demo environment.

---

## Story S6 — Backend Health

**As a** platform engineer
**I want** to see which database profile is currently active and whether the backend is reachable
**So that** I can confirm correct deployment configuration and diagnose data persistence issues without accessing server logs

### Acceptance Criteria

- [ ] AC1: The application info dropdown in the navigation bar displays the currently active Spring profile(s) under the "Profiles:" label; for example, a default startup with no profile flag shows an empty or `default` value, and a startup with `-Dspring.profiles.active=mysql` shows `mysql`.
- [ ] AC2: The "Services:" label in the same dropdown shows the name of any bound Cloud Foundry service or an empty value when running locally with no bound services.
- [ ] AC3: The data is sourced from the `GET /appinfo` endpoint; if that endpoint is unreachable, the dropdown labels render as blank rather than throwing a JavaScript error.
- [ ] AC4: `GET /appinfo` returns HTTP 200 with a JSON body that includes at minimum a `profiles` array and a `services` array.
- [ ] AC5: When the application is started with a profile requiring an external database (e.g., `mysql`) but the database is unreachable, the application fails to start with a clear error in the startup log — it does not start successfully and silently fail on the first API call.
- [ ] AC6: When running on the H2 profile, a warning is surfaced somewhere visible to operators (log line, `/appinfo` response field, or UI notice) indicating that data is not persisted across restarts.
- [ ] AC7: If more than one database profile is activated simultaneously (e.g., `mysql` and `mongodb`), the `SpringApplicationContextInitializer` rejects startup with a clear error message identifying the conflicting profiles.

### Priority Disagreements

| Stakeholder | Priority | Reasoning |
|-------------|----------|-----------|
| Platform Engineer | High | Without visibility into the active profile, a mis-configured deployment silently uses H2 (losing all data on restart) with no user-facing indication |
| Catalogue Manager | Low | Transparent to end users; the catalogue manager only cares that albums save and persist — the underlying backend is an ops concern |

**Unresolved disagreement — visibility of profile indicator:** The platform team wants the active profile prominently displayed, ideally in the main UI header always visible to operators. The catalogue team is concerned this exposes infrastructure details to end users and prefers the indicator be admin-only or hidden behind a separate `/admin` route. Neither position is adopted here.

**Unresolved disagreement — failover indicator:** The platform team would like an automated failover indicator that shows when the application has fallen back to H2 from a failed external database. No failover mechanism exists in the current codebase — `SpringApplicationContextInitializer` selects a profile at startup and cannot switch profiles at runtime. Any failover would require a restart; this disagreement is about whether that limitation should be surfaced in the UI.

### Out of Scope (this story)
- Automated health check endpoint compatible with Cloud Foundry health manager (Spring Actuator `/health`)
- Runtime profile switching without restart
- Alerting or paging integration
- Metrics or throughput monitoring

### Testing Notes
- Start the application with no profile flag and verify `GET /appinfo` shows `profiles: []` or `profiles: ["default"]` — the exact value depends on Spring Boot version and must be confirmed against the running application.
- Start with `-Dspring.profiles.active=mysql` pointing to a non-existent MySQL host and verify the process exits non-zero at startup.
- AC7 requires starting the application with two conflicting profile flags — do this in an isolated environment as it is expected to fail.

---

## Priority Matrix

All six stories ranked on two axes:

- **User-Facing Value:** How much does this capability matter to the end user or catalogue manager using the application day-to-day? (H = high, M = medium, L = low)
- **Modernisation Risk:** How likely is it that extracting this capability into a standalone microservice would break existing behaviour? (H = high risk of breakage, M = moderate, L = low risk)

| Story | Title | User-Facing Value | Modernisation Risk | Notes |
|-------|-------|-------------------|--------------------|-------|
| S1 | Browse Catalog | H | M | `GET /albums` is a simple `findAll()` — extraction is straightforward, but no pagination means the contract will need to change during extraction, risking frontend breakage |
| S2 | Add / Update Album | H | M | Write path is two separate HTTP methods (`PUT` add, `POST` update) with no authentication — extraction must preserve both methods and the frontend's implicit contract; adding auth during extraction raises risk |
| S3 | Delete Album | M | L | Single endpoint, no dependencies — lowest extraction risk; hard delete with no soft-delete flag means the extracted service has a simpler data model to replicate |
| S4 | View Recommendations | M | H | Does not yet exist; any implementation will introduce a new service dependency (genre query) and potentially a cross-service call back into the album catalog — highest risk of circular coupling during microservice extraction |
| S5 | Play History | L | H | Does not yet exist; requires identity, event store, and retention logic — building this inside the monolith first and then extracting would create the most tangled seam; greenfield extraction is lower risk but higher implementation cost |
| S6 | Backend Health | L | L | Read-only metadata endpoint with no domain logic — easiest to extract or replicate; lowest user-facing value because it is an ops concern invisible to end users |

**Ranking by combined priority (user value first, then extraction safety):**

1. S1 — Browse Catalog (H value, M risk) — implement and stabilise first
2. S2 — Add / Update Album (H value, M risk) — resolve auth disagreement before extracting
3. S3 — Delete Album (M value, L risk) — good candidate for early extraction practice
4. S4 — View Recommendations (M value, H risk) — design as a separate service from the start; do not implement inside the monolith
5. S6 — Backend Health (L value, L risk) — implement as part of ops tooling, not the catalogue roadmap
6. S5 — Play History (L value, H risk) — do not implement until identity model is decided; highest risk if implemented hastily
