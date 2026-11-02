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
package io.github.deepakdaneva.nifi.cli;

import io.github.deepakdaneva.nifi.cli.client.ConnectionSettings;
import io.github.deepakdaneva.nifi.cli.commands.AlignProcessGroups;
import io.github.deepakdaneva.nifi.cli.commands.ExitCodes;
import io.github.deepakdaneva.nifi.cli.commands.VersionProvider;
import io.github.deepakdaneva.nifi.cli.utils.AppUtils;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Top level {@code nifi-cli} command. Holds the connection options shared by all subcommands.
 * <p>
 * Authentication and logout around the selected subcommand are handled by {@link io.github.deepakdaneva.nifi.cli.commands.CliExecutionStrategy}.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@TopCommand
@CommandLine.Command(name = "nifi-cli", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, subcommands = {AlignProcessGroups.class, CommandLine.HelpCommand.class}, exitCodeListHeading = "%nExit Codes:%n", exitCodeList = {ExitCodes.LIST_OK, ExitCodes.LIST_SOFTWARE, ExitCodes.LIST_USAGE, ExitCodes.LIST_AUTHENTICATION})
public class MainCommand implements Runnable {

    @Spec
    CommandSpec spec;

    URI location;

    @CommandLine.Option(names = {Options.U, Options.USERNAME}, required = true, description = "Username of the user.")
    String username;

    @CommandLine.Option(names = {Options.P, Options.PASSWORD}, required = true, interactive = true, arity = "0..1", description = "Password of the user. Specify the option without a value to be prompted for it.")
    String password;

    @CommandLine.Option(names = {Options.K, Options.INSECURE}, description = "Disable TLS certificate and host name verification. INSECURE: only for testing against servers with self-signed certificates.")
    boolean insecure;

    @CommandLine.Option(names = {Options.TRUSTSTORE}, paramLabel = "<file>", description = "Trust store (PKCS12 or JKS) used to verify the NiFi server certificate. Defaults to the JVM trusted CAs.")
    Path trustStore;

    @CommandLine.Option(names = {Options.TRUSTSTORE_PASSWORD}, interactive = true, arity = "0..1", paramLabel = "<password>", description = "Password of the trust store. Specify the option without a value to be prompted for it.")
    String trustStorePassword;

    /**
     * Creates the command; picocli populates the options.
     */
    public MainCommand() {
    }

    /**
     * Returns the validated NiFi base URL.
     *
     * @return base NiFi URL ({@code scheme://host[:port]})
     */
    public URI getLocation() {
        return location;
    }

    /**
     * Validates and sets the NiFi base URL.
     *
     * @param value location given by the user
     * @throws ParameterException if the value is not a valid http(s) URL
     */
    @CommandLine.Option(names = {Options.L, Options.LOCATION}, required = true, description = "NiFi base url. (i.e. https://somehost.com:8443)")
    void setLocation(String value) {
        try {
            location = AppUtils.getURI(value, false);
        } catch (URISyntaxException e) {
            throw new ParameterException(spec.commandLine(), "Invalid NiFi URL (" + value + ") provided: " + e.getReason() + ".");
        }
    }

    /**
     * Returns the connection settings collected from the options.
     *
     * @return connection settings
     */
    public ConnectionSettings connectionSettings() {
        return new ConnectionSettings(location, username, password == null ? "" : password, insecure, trustStore, trustStorePassword);
    }

    /**
     * Only reached when no subcommand was given.
     *
     * @throws ParameterException always, to print the usage help
     */
    @Override
    public void run() {
        throw new ParameterException(spec.commandLine(), "No Command provided to Execute!");
    }

    /**
     * Option names of {@link MainCommand}.
     */
    public static final class Options {
        /**
         * Base NiFi URL (short form).
         */
        public static final String L = "-l";
        /**
         * Base NiFi URL.
         */
        public static final String LOCATION = "--location";
        /**
         * NiFi username (short form).
         */
        public static final String U = "-u";
        /**
         * NiFi username.
         */
        public static final String USERNAME = "--username";
        /**
         * NiFi password (short form).
         */
        public static final String P = "-p";
        /**
         * NiFi password.
         */
        public static final String PASSWORD = "--password";
        /**
         * Disable TLS verification (short form).
         */
        public static final String K = "-k";
        /**
         * Disable TLS verification.
         */
        public static final String INSECURE = "--insecure";
        /**
         * Trust store file.
         */
        public static final String TRUSTSTORE = "--truststore";
        /**
         * Trust store password.
         */
        public static final String TRUSTSTORE_PASSWORD = "--truststore-password";

        private Options() {
        }
    }
}
