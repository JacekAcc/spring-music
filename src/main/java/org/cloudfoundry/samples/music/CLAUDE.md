# Monolith Root — Claude Code Context

You are working inside the **legacy monolith**. This package is the extraction source, not the destination for new features.

## Primary rule
**Prefer extracting logic to `new-service/` over adding it here.** If a change touches album catalog read paths, it belongs in `catalog-service/` instead.

## Known technical debt (do not clean up without characterization tests green)
- `service/MusicCatalogFacade` — god class; CRUD, recommendations, audit, and user preferences all live here
- `service/RecommendationEngine` — circular dependency back into `MusicCatalogFacade` via `@Lazy`
- `service/AuditService` — trigger simulation via raw JDBC on every save
- `domain/Album` — carries user-tracking fields (`lastPlayedBy`, `playCount`, `recommendedFor`) that belong elsewhere
- `config/DataInitializationService` — mixes seed data, schema migration, and cache warming in one `@PostConstruct`

## Before any refactoring
Run the characterization suite first:
```bash
./gradlew test --tests "*.characterization.*"
```
All tests must stay green after your change. A failure means you broke a pinned behaviour.

## Service boundary
The monolith must never import from `org.cloudfoundry.catalog.*` (the extracted catalog service). Cross-service calls go through the strangler proxy in `web/AlbumController` via `RestTemplate`, controlled by the `catalog.service.enabled` flag.
