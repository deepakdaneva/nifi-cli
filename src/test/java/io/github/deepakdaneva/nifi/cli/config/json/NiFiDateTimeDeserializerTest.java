/*
 * Copyright (C) 2026 Deepak Kumar Jangir
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.github.deepakdaneva.nifi.cli.config.json;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import org.apache.nifi.web.api.dto.flow.ProcessGroupFlowDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NiFiDateTimeDeserializerTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-15T10:20:30.456Z"), KOLKATA);

    private final NiFiDateTimeDeserializer deserializer = new NiFiDateTimeDeserializer(CLOCK);

    @Test
    void parsesTimestampWithZone() {
        assertEquals(Date.from(Instant.parse("2023-10-11T12:34:56.789Z")), deserializer.parse("10/11/2023 12:34:56.789 UTC"));
    }

    @Test
    void parsesDateTimeWithZone() {
        assertEquals(Date.from(Instant.parse("2023-10-11T12:34:56Z")), deserializer.parse("10/11/2023 12:34:56 UTC"));
        assertEquals(Date.from(Instant.parse("2023-10-11T07:04:56Z")), deserializer.parse("10/11/2023 12:34:56 Asia/Kolkata"));
    }

    @Test
    void parsesTimeAgainstTodayInLocalZone() {
        // NiFi semantics: the zone text of a time-only value is ignored and the local zone is used
        assertEquals(Date.from(Instant.parse("2026-03-15T07:04:56Z")), deserializer.parse("12:34:56 UTC"));
    }

    @Test
    void parsesZoneOnlyAsCurrentInstant() {
        assertEquals(Date.from(Instant.parse("2026-03-15T10:20:30Z")), deserializer.parse("UTC"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void returnsNullForMissingValues(String value) {
        assertNull(deserializer.parse(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yesterday", "2023-10-11T12:34:56Z", "13/45/2023 99:99:99 UTC"})
    void rejectsUnknownFormats(String value) {
        assertThrows(JsonbException.class, () -> deserializer.parse(value));
    }

    @Test
    void worksAsJsonbDeserializerForNiFiDtos() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withDeserializers(deserializer))) {
            ProcessGroupFlowDTO dto = jsonb.fromJson("{\"id\":\"root\",\"lastRefreshed\":\"12:34:56 UTC\",\"unknownField\":true}", ProcessGroupFlowDTO.class);
            assertEquals("root", dto.getId());
            assertEquals(Date.from(Instant.parse("2026-03-15T07:04:56Z")), dto.getLastRefreshed());
        }
    }

    @Test
    void isThreadSafe() throws Exception {
        Date expected = Date.from(Instant.parse("2023-10-11T12:34:56.789Z"));
        Thread[] threads = new Thread[8];
        Throwable[] failure = new Throwable[1];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 500; i++) {
                    if (!expected.equals(deserializer.parse("10/11/2023 12:34:56.789 UTC"))) {
                        failure[0] = new AssertionError("Unexpected parse result");
                    }
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertNull(failure[0]);
    }
}
