# Scout Report — Seam Extraction Risk Analysis

Generated: 2026-04-24

## Methodology

Four subagent scouts independently scored each candidate seam against a five-dimension rubric. Scores are 1–5 (higher = more extraction risk). Weighted formula:

```
overall = coupling×0.3 + test×0.25 + data_tangle×0.2 + criticality×0.15 + reversibility×0.1
```

---

## Ranked Results (lowest extraction risk first)

| Rank | Seam | Coupling (×0.3) | Test Risk (×0.25) | Data Tangle (×0.2) | Criticality (×0.15) | Reversibility (×0.1) | Weighted Score | Verdict |
|------|------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| 1 | UserPreference | 1 | 5 | 1 | 1 | 1 | 2.0 | Low |
| 1 | RecommendationEngine | 1 | 5 | 1 | 1 | 1 | 2.0 | Low |
| 1 | AuditService | 1 | 5 | 1 | 1 | 1 | 2.0 | Low |
| 4 | AlbumCatalog | 1 | 5 | 2 | 4 | 2 | 2.75 | Low-Medium |

---

## Individual Seam Verdicts

### AlbumCatalog

```
Seam: AlbumCatalog
Coupling: 1 — AlbumController has one injected dependency (CrudRepository<Album,String>);
              no other class calls it; AlbumRepositoryPopulator resolves repository via
              BeanFactoryUtils at runtime, not compile-time
Test coverage: 5 — ApplicationTests.contextLoads() only; zero HTTP layer tests
Data-model tangle: 2 — Album entity shared across JPA/Mongo/Redis profiles; interface is clean
                       but AlbumRepositoryPopulator's BeanFactory resolution breaks if context
                       no longer contains a local CrudRepository
Business criticality: 4 — this IS the entire application; every user-visible operation flows
                          through AlbumController
Reversibility: 2 — strangler proxy (catalog.service.enabled flag) designed for this seam;
                   AlbumRepositoryPopulator rewrite required before flag flip
Overall extraction risk: 2.75 (Low-Medium)
Key blocker: AlbumRepositoryPopulator uses BeanFactoryUtils.beanOfTypeIncludingAncestors to
             resolve CrudRepository at runtime — breaks the moment the local repository is
             removed from the Spring context; must be rewritten before flag flip.
```

### UserPreference

```
Seam: UserPreference
Coupling: 1 — Fields lastPlayedBy, playCount, recommendedFor do NOT exist in Album.java;
              no callers, no references outside CLAUDE.md documentation
Test coverage: 5 — zero tests; the feature itself does not exist
Data-model tangle: 1 — no user-tracking fields in Album.java; no table sharing because there
                       is nothing to share yet
Business criticality: 1 — non-existent feature; zero current user-visible impact
Reversibility: 1 — nothing to extract; trivially reversible because nothing has been built
Overall extraction risk: 2.0 (Low)
Key blocker: The seam does not exist in code — it is documented intent in CLAUDE.md.
             Real work is implementing the fields first, which creates the tangle, then
             extracting them. Extraction risk score is misleadingly low — it measures a
             phantom seam.
```

### RecommendationEngine

```
Seam: RecommendationEngine
Coupling: 1 — service/ package does not exist; MusicCatalogFacade and RecommendationEngine
              classes are not implemented; no circular @Lazy dependency measurable in code
Test coverage: 5 — zero tests; feature does not exist
Data-model tangle: 1 — no service layer, no data model; nothing to tangle
Business criticality: 1 — no recommendation functionality in the running application
Reversibility: 1 — nothing to extract or reverse
Overall extraction risk: 2.0 (Low)
Key blocker: The seam is entirely aspirational — the service/ package, MusicCatalogFacade,
             and RecommendationEngine classes referenced in CLAUDE.md and the ADR do not
             exist in the actual source tree.
```

### AuditService

```
Seam: AuditService
Coupling: 1 — service/AuditService does not exist; no raw JDBC code anywhere in
              src/main/java
Test coverage: 5 — zero tests; feature does not exist
Data-model tangle: 1 — no audit tables, no JDBC DataSource usage in application code
Business criticality: 1 — no audit functionality in the running application
Reversibility: 1 — nothing to extract or reverse
Overall extraction risk: 2.0 (Low)
Key blocker: AuditService is documented debt, not implemented code. The ADR's stated reason
             for deferral (no event infrastructure) is valid as architectural guidance for
             when the class is eventually written — extraction risk today is zero.
```

---

## Surprising Findings

**Three of four seams are phantom seams.** UserPreference, RecommendationEngine, and AuditService reference classes and fields that do not exist in `src/main/java`. The ADR and CLAUDE.md treat them as existing technical debt, but the actual codebase contains only: `AlbumController`, `Album` (7 catalog fields — no user-tracking fields), three repository implementations, `AlbumRepositoryPopulator`, `SpringApplicationContextInitializer`, and `Application.java`. The `service/` package is empty.

**AlbumCatalog scores highest risk among the four**, despite the ADR ranking it as the easiest extraction (Rank 1). The inversion is explained entirely by `business_criticality` — `AlbumController` handles 100% of user-visible operations. The ADR's "Low" risk reflects architectural simplicity (clean interface, strangler proxy designed in).

**`AlbumRepositoryPopulator` is the single most concrete extraction blocker.** It uses `BeanFactoryUtils.beanOfTypeIncludingAncestors` — a raw ApplicationContext lookup rather than constructor injection. This breaks the moment the local `CrudRepository` bean is removed from the Spring context.

**`albums.json` embeds `_class: "org.cloudfoundry.samples.music.domain.Album"`** in every record. Jackson's `FAIL_ON_UNKNOWN_PROPERTIES=false` (set in `AlbumRepositoryPopulator`) handles it on read. But if the field propagates into outbound API responses, it leaks the monolith package name. The fence hook (Phase 5) must guard this.

---

## Comparison with Human ADR Ranking

### ADR Ranking

| Rank | Service | Overall Risk |
|------|---------|-------------|
| 1 | Album Catalog Service | Low |
| 2 | User Preference Service | Medium |
| 3 | Recommendation Engine Service | High |
| N/A | AuditService | Not extracted immediately |

### Scout vs. ADR

| Seam | Human Rank | Scout Rank | Agreement | Notes |
|------|-----------|------------|-----------|-------|
| AlbumCatalog | 1 (Low) | 4 (Low-Medium, 2.75) | Partial | Human: architectural simplicity; Scout: business criticality |
| UserPreference | 2 (Medium) | 1 (Low, 2.0) | Disagree — 1 rank | Scout finds no code; human scores anticipated future tangle |
| RecommendationEngine | 3 (High) | 1 (Low, 2.0) | **Disagree — 2 ranks (FLAGGED)** | Scout finds no code; human scores circular dep not yet implemented |
| AuditService | N/A (defer) | 1 (Low, 2.0) | Agree on deferral | Both say: do not extract now |

### Where They Agree

- **AlbumCatalog is the right first cut.** Both analyses converge on AlbumCatalog as the appropriate first extraction target. "Highest among four" at 2.75 is still a low absolute score — the direction of the recommendation is the same.
- **AuditService should be deferred.** The ADR excludes AuditService citing missing event infrastructure. The scout agrees — the class doesn't exist, and the architectural reasoning (synchronous HTTP callback negates decoupling) is sound.
- **Test coverage is universally absent.** Every seam scores 5 on test risk. This is the primary risk multiplier. The ADR correctly prescribes running Tester-Pin before any cut.

### Flagged Discrepancy: RecommendationEngine (2-rank delta)

The human ADR ranks RecommendationEngine as highest-risk extraction (3rd, "High") and states: "Cannot be extracted until the facade is broken apart and both ends have characterization coverage." The scout ranks it tied-first with a Low score.

This is not a disagreement about architecture — it is a disagreement about what is being measured.

The ADR's High risk rating is correct as a project-management signal: RecommendationEngine should be extracted last because the prerequisite work (implementing the service layer, characterization tests, facade decomposition) is substantial. The scout's Low score is correct as a code measurement: there is no implemented class with measurable coupling, data tangle, or criticality.

**Resolution:** The ADR scores the cost of extraction including the cost of building the thing to be extracted. The scout scores only the extraction mechanics of existing code. For sequencing purposes, the ADR's ranking is more useful. The scout's finding reveals that the documented risk lives entirely in the design specification, not yet in the source tree.

For the first extraction decision — what to cut now — both analyses agree: start with AlbumCatalog. It is the only seam where real code exists with real callers and real data.
