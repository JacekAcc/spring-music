package org.cloudfoundry.samples.music.characterization;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Placeholder characterization tests for RecommendationEngine.
 *
 * RecommendationEngine and MusicCatalogFacade are documented as planned technical debt
 * (see src/main/java/org/cloudfoundry/samples/music/CLAUDE.md) but do not yet exist in
 * src/main/java/org/cloudfoundry/samples/music/service/. These tests are disabled until
 * the service layer is implemented.
 *
 * When RecommendationEngine is implemented, remove @Disabled and pin the following behaviour:
 * - GET /recommendations returns the same list regardless of which user calls it (current known bug)
 * - Recommendations are based on album genre, not user history
 * - A user with no play history receives the same recommendations as any other user
 */
@Tag("characterization")
class RecommendationCharacterizationTest {

    @Test
    @Disabled("RecommendationEngine not yet implemented — pin when service/RecommendationEngine.java exists")
    void recommendations_areIdenticalRegardlessOfUser() {
        // BUG TO PIN: recommendations must be the same for user A and user B
        // (current design flaw — personalisation not implemented)
    }

    @Test
    @Disabled("RecommendationEngine not yet implemented — pin when service/RecommendationEngine.java exists")
    void recommendations_doNotLeakUserTrackingFields() {
        // Pin that lastPlayedBy, playCount, recommendedFor never appear in recommendation response
    }

    @Test
    @Disabled("MusicCatalogFacade circular dependency not yet implemented — "
            + "pin after @Lazy dep between RecommendationEngine and MusicCatalogFacade is resolved")
    void recommendationEngine_canBeInstantiatedWithoutFacade() {
        // Once the circular dependency is broken, RecommendationEngine must be independently testable
    }
}
