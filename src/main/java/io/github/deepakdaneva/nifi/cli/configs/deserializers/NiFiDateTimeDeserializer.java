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
package io.github.deepakdaneva.nifi.cli.configs.deserializers;

import jakarta.inject.Singleton;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import org.apache.nifi.web.api.dto.util.ParseDefaultingDateTimeFormatter;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@Singleton
public class NiFiDateTimeDeserializer implements JsonbDeserializer<Date> {

    /**
     * {@code z}
     */
    private static final String DEFAULT_ZONE_FORMAT = "z";
    /**
     * {@code HH:mm:ss z}
     */
    private static final String DEFAULT_TIME_FORMAT = "HH:mm:ss z";
    /**
     * {@code MM/dd/yyyy HH:mm:ss z}
     */
    private static final String DEFAULT_DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss z";
    /**
     * {@code MM/dd/yyyy HH:mm:ss.SSS z}
     */
    private static final String DEFAULT_TIMESTAMP_FORMAT = "MM/dd/yyyy HH:mm:ss.SSS z";
    /**
     * Default date time format
     */
    private static final SimpleDateFormat DATETIME_SIMPLE_DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
    /**
     * Default timestamp format
     */
    private static final SimpleDateFormat TIMESTAMP_SIMPLE_DATE_FORMAT = new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT, Locale.US);
    /**
     * Zone id
     */
    private static final ZoneId ZONE_ID = TimeZone.getDefault().toZoneId();

    static {
        DATETIME_SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getDefault());
        TIMESTAMP_SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getDefault());
    }

    /**
     * Time Formatter
     */
    private final ParseDefaultingDateTimeFormatter TIME_FORMATTER = new ParseDefaultingDateTimeFormatter(timestamp -> String.format("%s%s%s", timestamp.getYear(), timestamp.getMonthValue(), timestamp.getDayOfMonth()), timestamp -> new DateTimeFormatterBuilder().appendPattern(DEFAULT_TIME_FORMAT).parseDefaulting(ChronoField.YEAR, timestamp.getYear()).parseDefaulting(ChronoField.MONTH_OF_YEAR, timestamp.getMonthValue()).parseDefaulting(ChronoField.DAY_OF_MONTH, timestamp.getDayOfMonth()).parseDefaulting(ChronoField.MILLI_OF_SECOND, 0).toFormatter(Locale.US));

    /**
     * Deserialize JSON into object
     * 
     * @param parser parser
     * @param deserializationContext deserialization context
     * @param type type
     * @return deserialized date
     */
    @Override
    public Date deserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) {
        try {
            return timeDeserialize(parser, deserializationContext, type);
        } catch (Exception e1) {
            try {
                return dateTimeDeserialize(parser, deserializationContext, type);
            } catch (Exception e2) {
                try {
                    return timestampDeserialize(parser, deserializationContext, type);
                } catch (Exception e3) {
                    try {
                        return zoneDeserialize(parser, deserializationContext, type);
                    } catch (Exception ignored) {

                    }
                }
            }
        }
        throw new RuntimeException(new ParseException("Unable to parse value " + parser.getString(), 0));
    }

    /**
     * Deserialize time
     * 
     * @param parser parser
     * @param deserializationContext deserialization context
     * @param type type
     * @return deserialized date
     */
    private Date timeDeserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) {
        final DateTimeFormatter dtf = TIME_FORMATTER.get();
        final LocalDateTime parsedDateTime = LocalDateTime.parse(parser.getString(), dtf);
        final LocalDateTime now = LocalDateTime.now();
        return Date.from(parsedDateTime.toInstant(ZONE_ID.getRules().getOffset(now)));
    }

    /**
     * Deserialize date time
     * 
     * @param parser parser
     * @param deserializationContext deserialization context
     * @param type type
     * @return deserialized date
     * @throws ParseException parse exception
     */
    private Date dateTimeDeserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) throws ParseException {
        return DATETIME_SIMPLE_DATE_FORMAT.parse(parser.getString());
    }

    /**
     * Deserialize timestamp
     * 
     * @param parser parser
     * @param deserializationContext deserialization context
     * @param type type
     * @return deserialized date
     * @throws ParseException parse exception
     */
    private Date timestampDeserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) throws ParseException {
        return TIMESTAMP_SIMPLE_DATE_FORMAT.parse(parser.getString());
    }

    /**
     * Deserialize zone
     * 
     * @param parser parser
     * @param deserializationContext deserialization context
     * @param type type
     * @return deserialized date
     */
    private Date zoneDeserialize(JsonParser parser, DeserializationContext deserializationContext, Type type) {
        final LocalDateTime now = LocalDateTime.now();
        final DateTimeFormatter dtf = new DateTimeFormatterBuilder().appendPattern(DEFAULT_ZONE_FORMAT).parseDefaulting(ChronoField.YEAR, now.getYear()).parseDefaulting(ChronoField.MONTH_OF_YEAR, now.getMonthValue()).parseDefaulting(ChronoField.DAY_OF_MONTH, now.getDayOfMonth()).parseDefaulting(ChronoField.HOUR_OF_DAY, now.getHour()).parseDefaulting(ChronoField.MINUTE_OF_HOUR, now.getMinute()).parseDefaulting(ChronoField.SECOND_OF_MINUTE, now.getSecond()).parseDefaulting(ChronoField.MILLI_OF_SECOND, 0).toFormatter(Locale.US);
        final LocalDateTime parsedDateTime = LocalDateTime.parse(parser.getString(), dtf);
        return Date.from(parsedDateTime.toInstant(ZONE_ID.getRules().getOffset(now)));
    }
}
