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
package io.github.deepakdaneva.nifi.cli.commands;

import io.github.deepakdaneva.nifi.cli.client.NiFiSession;
import io.github.deepakdaneva.nifi.cli.service.ProcessGroupAligner;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * {@code align} subcommand: arranges independent process groups on the canvas in a grid.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 * @see ProcessGroupAligner
 */
@CommandLine.Command(name = "align", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, description = "Align independent process groups on the canvas in a grid manner.")
public class AlignProcessGroups implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {Options.D, Options.DEPTH}, defaultValue = "5", description = "Depth upto which process groups needs to be aligned. NOTE: Provide -1 to align all process groups recursively.")
    int givenDepth;

    @CommandLine.Option(names = {Options.R, Options.ROOTPGID}, description = "Root process group id from which aligning the process groups should be started down the hierarchy. NOTE: If not provided then root process group i.e. 'NIFI Flow' will be used.")
    String givenRootPgId;

    int columns;

    @Inject
    NiFiSession session;

    @Inject
    ProcessGroupAligner aligner;

    /**
     * Creates the command; picocli populates the options and CDI the dependencies.
     */
    public AlignProcessGroups() {
    }

    /**
     * Aligns the process groups using the authenticated session.
     */
    @Override
    public void run() {
        Log.info("Aligning Process Groups...");
        int moved = aligner.align(session.client(), givenRootPgId, givenDepth, columns);
        Log.infof("Aligning Completed! %d process group(s) aligned.", moved);
    }

    /**
     * Validates and sets the maximum number of grid columns.
     *
     * @param value number of columns given by the user
     * @throws CommandLine.ParameterException if the value is not an integer of at least 1
     */
    @CommandLine.Option(names = {Options.C, Options.COLUMNS}, defaultValue = "4", description = "Maximum number of columns. NOTE: This should not be less than 1.")
    void setColumns(String value) {
        int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException nfe) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Invalid columns number (" + value + ") provided.");
        }
        if (parsed < 1) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Maximum columns number can not be less than 1.");
        }
        columns = parsed;
    }

    /**
     * Option names of {@link AlignProcessGroups}.
     */
    public static final class Options {
        /**
         * Depth of the process groups (short form).
         */
        public static final String D = "-d";
        /**
         * Depth of the process groups.
         */
        public static final String DEPTH = "--depth";
        /**
         * Root process group id (short form).
         */
        public static final String R = "-r";
        /**
         * Root process group id.
         */
        public static final String ROOTPGID = "--rootpgid";
        /**
         * Grid columns to align process groups into (short form).
         */
        public static final String C = "-c";
        /**
         * Grid columns to align process groups into.
         */
        public static final String COLUMNS = "--columns";

        private Options() {
        }
    }
}
