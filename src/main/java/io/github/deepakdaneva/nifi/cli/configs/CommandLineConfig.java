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

import io.github.deepakdaneva.nifi.cli.configs.props.AppConfig;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@ApplicationScoped
public class CommandLineConfig {

    /**
     * Application configuration
     */
    @Inject
    AppConfig appConfig;

    /**
     * Initializes command line
     * 
     * @param factory to create command line instance
     * @return configured command line instance
     */
    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) {
        return factory.create().setExecutionStrategy(new CommandLine.RunAll()).setStopAtUnmatched(appConfig.cli().stopAtUnmatched()).setUnmatchedArgumentsAllowed(appConfig.cli().unmatchedArgumentsAllowed());
    }
}
