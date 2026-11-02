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

import io.github.deepakdaneva.nifi.cli.client.NiFiSession;
import io.github.deepakdaneva.nifi.cli.commands.CliExceptionHandler;
import io.github.deepakdaneva.nifi.cli.commands.CliExecutionStrategy;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Produces the configured picocli {@link CommandLine} that Quarkus uses to run the application.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@ApplicationScoped
public class CommandLineConfig {

    @Inject
    AppConfig appConfig;

    @Inject
    NiFiSession session;

    /**
     * Creates the configuration bean; dependencies are injected by CDI.
     */
    public CommandLineConfig() {
    }

    /**
     * Creates and configures the command line.
     *
     * @param factory Quarkus factory that creates the command line for the top command
     * @return configured command line instance
     */
    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) {
        return factory.create().setExecutionStrategy(new CliExecutionStrategy(session)).setExecutionExceptionHandler(new CliExceptionHandler()).setStopAtUnmatched(appConfig.cli().stopAtUnmatched()).setUnmatchedArgumentsAllowed(appConfig.cli().unmatchedArgumentsAllowed());
    }
}
