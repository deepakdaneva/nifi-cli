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

import io.github.deepakdaneva.nifi.cli.configs.AppRestClientConfig;
import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import io.github.deepakdaneva.nifi.cli.utils.AppUtils;
import io.github.deepakdaneva.nifi.cli.services.NiFiService;
import io.github.deepakdaneva.nifi.cli.subcmds.AlignProcessGroups;
import io.quarkus.logging.Log;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Spec;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@TopCommand
@CommandLine.Command(name = "nifi-cli", version = "1.0.0", mixinStandardHelpOptions = true, subcommands = {AlignProcessGroups.class})
public class MainCommand implements Runnable {

    /**
     * Command Spec
     */
    @Spec
    CommandSpec spec;

    /**
     * NiFi Base URL
     */
    URI location;
    /**
     * NiFi Username
     */
    @CommandLine.Option(names = {Options.U, Options.USERNAME}, required = true, description = "Username of the user.")
    String username;
    /**
     * NiFi Password
     */
    @CommandLine.Option(names = {Options.P, Options.PASSWORD}, required = true, description = "Password of the user.")
    String password;
    /**
     * NiFi Service
     */
    @Inject
    @RestClient
    NiFiService nifiService;

    /**
     * Generates the URI using the provided {@code location} string
     * 
     * @return Base NiFi REST APIs URI
     */
    public URI getLocation() {
        try {
            return new URI(location.toString());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Set the ${@link MainCommand#location} by using the provided location string by the user.
     * 
     * @param value string location
     * @throws Exception if location is not of a valid pattern
     */
    @CommandLine.Option(names = {Options.L, Options.LOCATION}, required = true, description = "NiFi base url. (i.e. https://somehost.com:8443)")
    void setLocation(String value) throws Exception {
        location = AppUtils.getURI(value, true);
        if (location == null) {
            throw new ParameterException(spec.commandLine(), "Invalid NiFi URL (" + value + ") provided.");
        }
    }

    /**
     * Actual command logic to execute
     */
    @Override
    public void run() {
        ParseResult subCmd = spec.commandLine().getParseResult().subcommand();
        if (subCmd != null) {
            try {
                Log.info("Authenticating...");
                AppRestClientConfig.accessToken = nifiService.getAccessToken(username, password);
                Log.info("Authenticated!");
            } catch (Exception e) {
                Log.error(e.getMessage());
                System.exit(CommandLine.ExitCode.SOFTWARE);
            }
        } else {
            throw new CommandLine.ParameterException(spec.commandLine(), "No Command provided to Execute!");
        }
    }

    /**
     * This method will be invoked by Quarkus when application shuts down.
     *
     * @param se shutdown event to execute logic when application shuts down
     */
    public void onShutdown(@Observes ShutdownEvent se) {
        try {
            if (AppRestClientConfig.accessToken != null) {
                try {
                    nifiService.logout();
                } catch (NiFiResponseException e) {
                    if (se.isStandardShutdown()) {
                        Log.warn("Unable to logout: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            if (se.isStandardShutdown()) {
                Log.warn("Unable to logout: " + e.getMessage());
                Log.debug(e.getMessage());
            }
        } finally {
            AppRestClientConfig.accessToken = null;
        }
    }

    /**
     * Options for {@link io.github.deepakdaneva.nifi.cli.MainCommand}
     */
    public static final class Options {
        /**
         * Base NiFi URL
         */
        public static final String L = "-l";
        /**
         * Base NiFi URL
         */
        public static final String LOCATION = "--location";
        /**
         * NiFi username
         */
        public static final String U = "-u";
        /**
         * NiFi username
         */
        public static final String USERNAME = "--username";
        /**
         * NiFi password
         */
        public static final String P = "-p";
        /**
         * NiFi password
         */
        public static final String PASSWORD = "--password";
    }
}