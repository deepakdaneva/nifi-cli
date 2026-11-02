/*
 * Copyright (C) 2023 Deepak Kumar Jangir
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

import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import java.lang.reflect.Type;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Deserializes the date/time strings produced by the NiFi REST API into {@link Date}.
 * <p>
 * NiFi serializes {@code Date} fields through JAXB adapters, which JSON-B does not honour. The supported formats mirror those adapters and are
 * tried in this order:
 * <ol>
 * <li>{@code MM/dd/yyyy HH:mm:ss.SSS z} (timestamp)</li>
 * <li>{@code MM/dd/yyyy HH:mm:ss z} (date and time)</li>
 * <li>{@code HH:mm:ss z} (time only; resolved against today's date in the local time zone, as NiFi does)</li>
 * <li>{@code z} (time zone only; resolved to the current time truncated to seconds)</li>
 * </ol>
 * Instances are thread safe.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@Singleton
public class NiFiDateTimeDeserializer implements JsonbDeserializer<Date> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS z", Locale.US);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss z", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss z", Locale.US);
    private static final DateTimeFormatter ZONE_FORMAT = DateTimeFormatter.ofPattern("z", Locale.US);

    private final Clock clock;

    /**
     * Creates a deserializer that uses the system clock and the default time zone.
     */
    public NiFiDateTimeDeserializer() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Creates a deserializer with an explicit clock; the clock's zone is used for values that carry no date.
     *
     * @param clock clock providing the current date and the local time zone
     */
    public NiFiDateTimeDeserializer(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Deserializes the current JSON string value.
     *
     * @param parser parser positioned on the value
     * @param deserializationContext deserialization context
     * @param type target type
     * @return parsed date
     * @throws JsonbException if the value does not match any supported format
     */
    @Override
    public Date deserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) {
        return parse(parser.getString());
    }

    /**
     * Parses a NiFi formatted date/time string.
     *
     * @param value value to parse; {@code null} or blank yields {@code null}
     * @return parsed date, or {@code null} for a {@code null} or blank value
     * @throws JsonbException if the value does not match any supported format
     */
    public Date parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            return Date.from(ZonedDateTime.parse(text, TIMESTAMP_FORMAT).toInstant());
        } catch (DateTimeParseException ignored) {
            // not a timestamp, try the next format
        }
        try {
            return Date.from(ZonedDateTime.parse(text, DATE_TIME_FORMAT).toInstant());
        } catch (DateTimeParseException ignored) {
            // not a date time, try the next format
        }
        try {
            LocalTime time = LocalTime.parse(text, TIME_FORMAT);
            return Date.from(LocalDate.now(clock).atTime(time).atZone(clock.getZone()).toInstant());
        } catch (DateTimeParseException ignored) {
            // not a time, try the next format
        }
        try {
            ZONE_FORMAT.parse(text);
            return Date.from(clock.instant().truncatedTo(ChronoUnit.SECONDS));
        } catch (DateTimeParseException e) {
            throw new JsonbException("Unable to parse NiFi date value '" + text + "'", e);
        }
    }
}
