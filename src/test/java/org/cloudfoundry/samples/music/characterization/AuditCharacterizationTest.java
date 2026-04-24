package org.cloudfoundry.samples.music.characterization;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Placeholder characterization tests for AuditService.
 *
 * AuditService is documented as planned technical debt
 * (see src/main/java/org/cloudfoundry/samples/music/CLAUDE.md) but does not yet exist in
 * src/main/java/org/cloudfoundry/samples/music/service/. These tests are disabled until
 * the service is implemented.
 *
 * When AuditService is implemented, remove @Disabled and pin the following behaviour:
 * - Every album save (PUT or POST /albums) writes exactly one audit row
 * - Each audit row contains: albumId (String), timestamp (not null), operation type
 * - Audit rows are written via raw JDBC (not via Spring Data) — verify with DataSource directly
 * - A failed save must not write a partial audit row (transactional boundary)
 */
@Tag("characterization")
class AuditCharacterizationTest {

    @Test
    @Disabled("AuditService not yet implemented — pin when service/AuditService.java exists")
    void save_writesExactlyOneAuditRow() {
        // Pin: PUT /albums writes exactly 1 audit row with albumId and timestamp
    }

    @Test
    @Disabled("AuditService not yet implemented — pin when service/AuditService.java exists")
    void auditRow_containsAlbumIdAndTimestamp() {
        // Pin field names: audit table must have columns 'albumId' and 'timestamp'
        // If column names change, extraction contract breaks
    }

    @Test
    @Disabled("AuditService not yet implemented — pin when service/AuditService.java exists")
    void failedSave_doesNotWriteAuditRow() {
        // Pin transactional safety: a rollback must not leave orphaned audit rows
    }
}
