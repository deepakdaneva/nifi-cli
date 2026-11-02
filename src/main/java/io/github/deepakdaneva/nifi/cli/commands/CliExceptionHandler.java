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

import io.github.deepakdaneva.nifi.cli.exceptions.AuthenticationException;
import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import io.quarkus.logging.Log;
import jakarta.ws.rs.ProcessingException;
import picocli.CommandLine;

/**
 * Turns exceptions raised while executing a command into a short error message and an exit code from {@link ExitCodes}.
 * <p>
 * Stack traces are only logged at debug level.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
public class CliExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    /**
     * Creates the handler.
     */
    public CliExceptionHandler() {
    }

    /**
     * Logs the failure and selects the exit code.
     *
     * @param ex exception thrown by the command
     * @param commandLine command that failed
     * @param parseResult parse result of the command line
     * @return exit code
     */
    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        Log.debug("Command failed", ex);
        if (ex instanceof AuthenticationException) {
            Log.error(ex.getMessage());
            return ExitCodes.AUTHENTICATION;
        }
        if (ex instanceof NiFiResponseException nre) {
            Log.error("NiFi request failed (HTTP " + nre.getStatus() + "): " + nre.getMessage());
            return ExitCodes.SOFTWARE;
        }
        if (ex instanceof ProcessingException) {
            Log.error("Unable to communicate with NiFi: " + rootMessage(ex));
            return ExitCodes.SOFTWARE;
        }
        Log.error(rootMessage(ex));
        return ExitCodes.SOFTWARE;
    }

    private static String rootMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
