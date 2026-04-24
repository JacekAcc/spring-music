package org.cloudfoundry.samples.music.characterization;

import org.cloudfoundry.samples.music.domain.Album;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("characterization")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlbumCrudCharacterizationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/albums";
    }

    @Test
    void listAll_returns200AndNonEmptyList() {
        ResponseEntity<List<Album>> response = restTemplate.exchange(
                baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {});

        assertThat(response.getStatusCode())
                .as("GET /albums must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("GET /albums must return a non-empty list (seeded from albums.json)")
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void listAll_returns29SeedAlbums() {
        ResponseEntity<List<Album>> response = restTemplate.exchange(
                baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {});

        assertThat(response.getBody())
                .as("GET /albums must return exactly 29 albums seeded from albums.json")
                .hasSize(29);
    }

    @Test
    void add_usesHttpPut_returns200() {
        // BUG PINNED: AlbumController uses PUT for create (not POST), contrary to REST convention.
        // This test pins the current behaviour — do not "fix" without updating this test.
        Album newAlbum = new Album("Test Title", "Test Artist", "2000", "Rock");

        ResponseEntity<Album> response = restTemplate.exchange(
                baseUrl(), HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(newAlbum),
                Album.class);

        assertThat(response.getStatusCode())
                .as("PUT /albums (create) must return 200 — verb is intentionally swapped vs REST convention")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("PUT /albums response body must not be null")
                .isNotNull();
        assertThat(response.getBody().getId())
                .as("PUT /albums must assign an id to the created album")
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void update_usesHttpPost_returns200() {
        // BUG PINNED: AlbumController uses POST for update (not PUT), contrary to REST convention.
        Album toCreate = new Album("Update Me", "Artist", "1999", "Jazz");
        Album created = restTemplate.exchange(
                baseUrl(), HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(toCreate),
                Album.class).getBody();

        assertThat(created).as("Precondition: album must be created before update").isNotNull();

        created.setTitle("Updated Title");
        ResponseEntity<Album> response = restTemplate.exchange(
                baseUrl(), HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(created),
                Album.class);

        assertThat(response.getStatusCode())
                .as("POST /albums (update) must return 200 — verb is intentionally swapped vs REST convention")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle())
                .as("POST /albums must persist the updated title")
                .isEqualTo("Updated Title");
    }

    @Test
    void getById_existingId_returns200() {
        List<Album> albums = restTemplate.exchange(
                baseUrl(), HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {}).getBody();
        String id = albums.get(0).getId();

        ResponseEntity<Album> response = restTemplate.getForEntity(baseUrl() + "/" + id, Album.class);

        assertThat(response.getStatusCode())
                .as("GET /albums/{id} for an existing id must return 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId())
                .as("GET /albums/{id} response body must contain the requested id")
                .isEqualTo(id);
    }

    @Test
    void getById_missingId_returns200WithNullBody() {
        // BUG PINNED: AlbumController.getById calls .orElse(null) and Spring serialises
        // null as an empty body with HTTP 200, not 404. Pin this so we notice if it changes.
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/nonexistent-id-xyz", String.class);

        assertThat(response.getStatusCode())
                .as("GET /albums/{id} for missing id must return 200 (not 404) — bug pinned")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteById_existingId_returns200() {
        Album toDelete = new Album("Delete Me", "Artist", "2001", "Pop");
        Album created = restTemplate.exchange(
                baseUrl(), HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(toDelete),
                Album.class).getBody();

        assertThat(created).as("Precondition: album must be created before delete").isNotNull();

        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(response.getStatusCode())
                .as("DELETE /albums/{id} for an existing id must return 200")
                .isEqualTo(HttpStatus.OK);
    }
}
