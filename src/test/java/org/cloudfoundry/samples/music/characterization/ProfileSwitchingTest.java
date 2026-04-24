package org.cloudfoundry.samples.music.characterization;

import org.cloudfoundry.samples.music.domain.Album;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("characterization")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileSwitchingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void h2Profile_bootsSuccessfully() {
        // No active profile = H2 in-memory. If this fails, something changed in context init.
        List<Album> albums = restTemplate.exchange(
                "http://localhost:" + port + "/albums",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {}).getBody();

        assertThat(albums)
                .as("H2 profile must boot and return a non-null album list")
                .isNotNull();
    }

    @Test
    void h2Profile_seeds29Albums() {
        List<Album> albums = restTemplate.exchange(
                "http://localhost:" + port + "/albums",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {}).getBody();

        assertThat(albums)
                .as("H2 profile must seed exactly 29 albums from albums.json on startup")
                .hasSize(29);
    }

    @Test
    void h2Profile_seededAlbums_haveRequiredFields() {
        List<Album> albums = restTemplate.exchange(
                "http://localhost:" + port + "/albums",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Album>>() {}).getBody();

        assertThat(albums).as("Precondition: albums must be seeded").isNotEmpty();
        Album first = albums.get(0);

        assertThat(first.getId())
                .as("Seeded album must have an id assigned by RandomIdGenerator")
                .isNotNull()
                .isNotEmpty();
        assertThat(first.getTitle())
                .as("Seeded album must have a non-null title")
                .isNotNull();
        assertThat(first.getArtist())
                .as("Seeded album must have a non-null artist")
                .isNotNull();
        assertThat(first.getReleaseYear())
                .as("Seeded album 'releaseYear' must be a non-null String")
                .isNotNull();
        assertThat(first.getGenre())
                .as("Seeded album must have a non-null genre")
                .isNotNull();
    }
}
