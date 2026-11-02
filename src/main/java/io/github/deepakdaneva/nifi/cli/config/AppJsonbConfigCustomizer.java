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
package io.github.deepakdaneva.nifi.cli.config;

import io.github.deepakdaneva.nifi.cli.config.json.NiFiDateTimeDeserializer;
import io.quarkus.jsonb.JsonbConfigCustomizer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;

/**
 * Registers NiFi specific JSON-B customizations with the JSON-B instance used by the REST client.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@Singleton
public class AppJsonbConfigCustomizer implements JsonbConfigCustomizer {

    @Inject
    NiFiDateTimeDeserializer niFiDateTimeDeserializer;

    /**
     * Creates the customizer; dependencies are injected by CDI.
     */
    public AppJsonbConfigCustomizer() {
    }

    /**
     * Adds the NiFi date/time deserializer to the given configuration.
     *
     * @param jsonbConfig configuration to customize
     */
    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withDeserializers(niFiDateTimeDeserializer);
    }
}
