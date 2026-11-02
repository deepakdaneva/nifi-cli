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
package io.github.deepakdaneva.nifi.cli.commands;

import io.github.deepakdaneva.nifi.cli.MainCommand;
import io.github.deepakdaneva.nifi.cli.client.NiFiSession;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

import java.util.Objects;

/**
 * Runs the selected subcommand inside an authenticated NiFi session.
 * <p>
 * Help and version requests are answered without contacting NiFi. Otherwise the strategy logs in with the options of {@link MainCommand}, runs
 * the last (most specific) subcommand and always logs out afterwards.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
public class CliExecutionStrategy implements CommandLine.IExecutionStrategy {

    private final NiFiSession session;

    /**
     * Creates the strategy.
     *
     * @param session session used to log in and out
     */
    public CliExecutionStrategy(NiFiSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /**
     * Executes the parsed command line.
     *
     * @param parseResult parse result of the top command
     * @return exit code
     * @throws CommandLine.ExecutionException if logging in or the subcommand fails
     * @throws CommandLine.ParameterException if no subcommand was given
     */
    @Override
    public int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) {
            return helpExitCode;
        }
        CommandLine commandLine = parseResult.commandSpec().commandLine();
        if (!parseResult.hasSubcommand()) {
            throw new CommandLine.ParameterException(commandLine, "No Command provided to Execute!");
        }
        MainCommand main = (MainCommand) parseResult.commandSpec().userObject();
        try {
            try {
                session.open(main.connectionSettings());
            } catch (RuntimeException e) {
                throw new CommandLine.ExecutionException(commandLine, e.getMessage(), e);
            }
            return new CommandLine.RunLast().execute(parseResult);
        } finally {
            session.close();
        }
    }
}
