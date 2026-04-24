# Extracted Catalog Service — Claude Code Context

You are working inside the **extracted catalog service**. This is a clean bounded context; the monolith's internal model must never leak in here.

## Primary rule
**Never reference monolith internal packages** (`org.cloudfoundry.samples.music.*`). All data enters through the ACL translator in `acl/AlbumTranslator`.

## Prohibited field names in the public API
The following field names must never appear in any API response JSON or DTO:
- `lastPlayedBy`
- `playCount`
- `recommendedFor`
- `_class`

If you are about to add one of these, stop — it means monolith concerns are leaking across the boundary.

## Clean domain model
`domain/CatalogAlbum` contains exactly: `id`, `title`, `artist`, `releaseYear`, `genre`, `trackCount`. No JPA annotations, no user-tracking fields.

## Testing
- `@Tag("contract")` — API shape tests; must stay green alongside monolith characterization suite
- `fence/BoundaryLeakTest` — scans API response for prohibited field names; must always pass

## Port
Catalog service runs on **port 8081**. Monolith stays on 8080.
