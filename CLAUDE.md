# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
