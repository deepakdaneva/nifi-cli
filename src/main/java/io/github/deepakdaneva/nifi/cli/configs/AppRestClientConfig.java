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

import io.github.deepakdaneva.nifi.cli.MainCommand;
import io.github.deepakdaneva.nifi.cli.services.NiFiService;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@ApplicationScoped
public class AppRestClientConfig {

    /**
     * Access token
     */
    public static String accessToken;

    /**
     * Generates NiFi Rest Client
     * 
     * @param parseResult to get base uri of NiFi server
     * @return NiFi Rest Client
     */
    @Produces
    @RestClient
    @ApplicationScoped
    NiFiService restClient(CommandLine.ParseResult parseResult) {
        MainCommand mcmd = (MainCommand) parseResult.commandSpec().userObject();
        return QuarkusRestClientBuilder.newBuilder().baseUri(mcmd.getLocation()).build(NiFiService.class);
    }
}
