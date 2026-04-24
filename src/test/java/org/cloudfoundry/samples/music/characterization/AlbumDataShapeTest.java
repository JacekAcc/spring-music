package org.cloudfoundry.samples.music.characterization;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("characterization")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlbumDataShapeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/albums";
    }

    @Test
    @SuppressWarnings("unchecked")
    void albumResponse_containsExpectedFields() {
        List<LinkedHashMap<String, Object>> albums = restTemplate.getForObject(baseUrl(), List.class);

        assertThat(albums).as("Album list must not be empty").isNotEmpty();
        LinkedHashMap<String, Object> album = albums.get(0);

        assertThat(album).as("Response must contain field 'id'").containsKey("id");
        assertThat(album).as("Response must contain field 'title'").containsKey("title");
        assertThat(album).as("Response must contain field 'artist'").containsKey("artist");
        assertThat(album).as("Response must contain field 'releaseYear'").containsKey("releaseYear");
        assertThat(album).as("Response must contain field 'genre'").containsKey("genre");
        assertThat(album).as("Response must contain field 'trackCount'").containsKey("trackCount");
        assertThat(album).as("Response must contain field 'albumId'").containsKey("albumId");
    }

    @Test
    @SuppressWarnings("unchecked")
    void albumResponse_doesNotContainMonolithInternalFields() {
        // These fields are banned from the public API per service boundary rules in CLAUDE.md.
        // They do not exist in Album.java today — this test pins their absence so any
        // accidental addition is caught before the catalog service is extracted.
        List<LinkedHashMap<String, Object>> albums = restTemplate.getForObject(baseUrl(), List.class);

        assertThat(albums).as("Album list must not be empty").isNotEmpty();
        LinkedHashMap<String, Object> album = albums.get(0);

        assertThat(album).as("Field 'lastPlayedBy' must not appear in API response (monolith-internal — banned by fence)")
                .doesNotContainKey("lastPlayedBy");
        assertThat(album).as("Field 'playCount' must not appear in API response (monolith-internal — banned by fence)")
                .doesNotContainKey("playCount");
        assertThat(album).as("Field 'recommendedFor' must not appear in API response (monolith-internal — banned by fence)")
                .doesNotContainKey("recommendedFor");
        assertThat(album).as("Field '_class' must not appear in API response (serialisation leak from albums.json)")
                .doesNotContainKey("_class");
    }

    @Test
    @SuppressWarnings("unchecked")
    void albumResponse_trackCount_isNumeric() {
        // trackCount is a primitive int in Album.java — it serialises as a number, never null.
        List<LinkedHashMap<String, Object>> albums = restTemplate.getForObject(baseUrl(), List.class);

        assertThat(albums).as("Album list must not be empty").isNotEmpty();
        for (LinkedHashMap<String, Object> album : albums) {
            assertThat(album.get("trackCount"))
                    .as("Field 'trackCount' must be an Integer (primitive int, never null) for album: " + album.get("title"))
                    .isInstanceOf(Integer.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void albumResponse_releaseYear_isString() {
        // releaseYear is typed as String in Album.java, not int — pin this so type changes are caught.
        List<LinkedHashMap<String, Object>> albums = restTemplate.getForObject(baseUrl(), List.class);

        assertThat(albums).as("Album list must not be empty").isNotEmpty();
        LinkedHashMap<String, Object> album = albums.get(0);

        assertThat(album.get("releaseYear"))
                .as("Field 'releaseYear' must be a String (not int) — typed as String in Album.java")
                .isInstanceOf(String.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void albumResponse_seededAlbums_haveNullAlbumId() {
        // albums.json does not set albumId — seeded albums have null albumId.
        // Pin this so we notice if the populator starts setting it.
        List<LinkedHashMap<String, Object>> albums = restTemplate.getForObject(baseUrl(), List.class);

        assertThat(albums).as("Album list must not be empty").isNotEmpty();
        long nullAlbumIdCount = albums.stream()
                .filter(a -> a.get("albumId") == null)
                .count();

        assertThat(nullAlbumIdCount)
                .as("All 29 seeded albums must have null albumId (not set in albums.json)")
                .isEqualTo(29);
    }
}
