package com.geli.warehouse.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * SQLite stores timestamps with millisecond precision, so timestamps are
 * truncated at the source. A freshly created response then always matches what
 * a later read returns.
 */
public final class Timestamps {

    private Timestamps() {
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
