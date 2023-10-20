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
package io.github.deepakdaneva.nifi.cli.configs.props;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@ConfigMapping(prefix = "io.github.deepakdaneva.nifi")
public interface AppConfig {

    /**
     * CLI relation configurations
     * 
     * @return provided cli related properties pojo instance
     */
    Cli cli();

    /**
     * CLI relation configurations
     */
    interface Cli {
        /**
         * Property whether to fail the CLI execution if an unmatched option is provided
         * 
         * @return boolean value set for this property, default is {@code false}
         */
        @WithDefault("false")
        boolean stopAtUnmatched();

        /**
         * Property whether unmatched arguments are allowed or not
         * 
         * @return boolean value set for this property, default is {@code true}
         */
        @WithDefault("true")
        boolean unmatchedArgumentsAllowed();
    }
}
