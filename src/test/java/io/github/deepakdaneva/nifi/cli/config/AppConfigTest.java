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
package io.github.deepakdaneva.nifi.cli.config;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    private static AppConfig load(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(AppConfig.class).withSources(new PropertiesConfigSource(properties, "test", 500)).build();
        return config.getConfigMapping(AppConfig.class);
    }

    @Test
    void appliesDefaults() {
        AppConfig config = load(Map.of());
        assertFalse(config.cli().stopAtUnmatched());
        assertTrue(config.cli().unmatchedArgumentsAllowed());
    }

    @Test
    void readsDocumentedPropertyNames() {
        AppConfig config = load(Map.of("io.github.deepakdaneva.nifi.cli.stop-at-unmatched", "true", "io.github.deepakdaneva.nifi.cli.unmatched-arguments-allowed", "false"));
        assertTrue(config.cli().stopAtUnmatched());
        assertFalse(config.cli().unmatchedArgumentsAllowed());
    }

    @Test
    void bundledApplicationPropertiesMatchTheMapping() throws IOException {
        URL url = AppConfigTest.class.getClassLoader().getResource("application.properties");
        assertNotNull(url);
        SmallRyeConfig config = new SmallRyeConfigBuilder().withMapping(AppConfig.class).withSources(new PropertiesConfigSource(url)).build();
        AppConfig appConfig = config.getConfigMapping(AppConfig.class);
        assertFalse(appConfig.cli().stopAtUnmatched());
        assertTrue(appConfig.cli().unmatchedArgumentsAllowed());
    }

    @Test
    void rejectsUnknownPropertiesInTheNamespace() {
        assertThrows(RuntimeException.class, () -> load(Map.of("io.github.deepakdaneva.nifi.cli.stop-at-unmatchd", "true")));
    }
}
