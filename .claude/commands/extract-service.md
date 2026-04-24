Extract the named seam as a standalone Spring Boot service alongside the monolith.

Seam to extract: $ARGUMENTS

Follow these steps in order:

1. **Verify characterization suite is green** before touching any code:
   ```bash
   ./gradlew test --tests "*.characterization.*"
   ```
   Stop and report if any test fails.

2. **Locate the seam** — find all classes in the monolith that belong to `$ARGUMENTS`. List them with their package paths and direct dependencies.

3. **Create the new service module** under `catalog-service/` (or a name matching the seam):
   - `build.gradle` with Spring Boot 2.4 + Spring Data JPA + H2
   - Clean DTO in `api/` — no JPA annotations, no monolith field names (`lastPlayedBy`, `playCount`, `recommendedFor`, `_class`)
   - Pure domain class with only the fields that belong to this bounded context
   - Repository extending `CrudRepository`
   - Controller on port 8081
   - Contract test annotated `@Tag("contract")`

4. **Add the strangler proxy** in the monolith's `AlbumController`:
   - Inject `RestTemplate catalogServiceClient`
   - When `catalog.service.enabled=true`, proxy GET requests to the new service
   - When false, fall back to the local repository
   - Add the flag to `application.yml` defaulting to `false`

5. **Add ACL translator** in `acl/AlbumTranslator` — the only place that maps monolith types to the new service's API types.

6. **Verify both suites green on the same commit**:
   ```bash
   ./gradlew test
   ```
   Both `@Tag("characterization")` and `@Tag("contract")` must pass.

7. Report: which classes moved, what the new API contract looks like, and any coupling that couldn't be cleanly cut.
