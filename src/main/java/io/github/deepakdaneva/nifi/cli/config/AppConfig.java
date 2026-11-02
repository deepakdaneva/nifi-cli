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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Application specific configuration, mapped from properties with the prefix {@code io.github.deepakdaneva.nifi}.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@ConfigMapping(prefix = "io.github.deepakdaneva.nifi")
public interface AppConfig {

    /**
     * Command line parsing configuration ({@code io.github.deepakdaneva.nifi.cli.*}).
     *
     * @return command line parsing configuration
     */
    Cli cli();

    /**
     * Command line parsing configuration.
     */
    interface Cli {
        /**
         * Whether option parsing stops at the first unmatched argument ({@code io.github.deepakdaneva.nifi.cli.stop-at-unmatched}).
         *
         * @return configured value, default is {@code false}
         */
        @WithDefault("false")
        boolean stopAtUnmatched();

        /**
         * Whether unmatched arguments are tolerated instead of failing the command ({@code io.github.deepakdaneva.nifi.cli.unmatched-arguments-allowed}).
         *
         * @return configured value, default is {@code false}
         */
        @WithDefault("false")
        boolean unmatchedArgumentsAllowed();
    }
}
