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
package io.github.deepakdaneva.nifi.cli.configs;

import io.github.deepakdaneva.nifi.cli.configs.deserializers.NiFiDateTimeDeserializer;
import io.quarkus.jsonb.JsonbConfigCustomizer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@Singleton
public class AppJsonbConfigCustomizer implements JsonbConfigCustomizer {

    /**
     * NiFi specific date time deserializer
     */
    @Inject
    NiFiDateTimeDeserializer niFiDateTimeDeserializer;

    /**
     * Customizes the behaviour of deserialization
     * 
     * @param jsonbConfig to configure
     */
    @Override
    public void customize(JsonbConfig jsonbConfig) {
        jsonbConfig.withDeserializers(niFiDateTimeDeserializer);
    }
}
