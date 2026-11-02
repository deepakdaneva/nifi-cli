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
package io.github.deepakdaneva.nifi.cli.client;

import io.github.deepakdaneva.nifi.cli.exceptions.AuthenticationException;
import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Holds the authenticated connection to one NiFi instance for the lifetime of a CLI invocation.
 * <p>
 * {@link #open(ConnectionSettings)} builds the REST client and logs in; {@link #close()} logs out again. The access token is kept in memory only
 * and is never logged.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
@ApplicationScoped
public class NiFiSession {

    private NiFiClient client;
    private volatile String accessToken;

    /**
     * Creates an unopened session.
     */
    public NiFiSession() {
    }

    /**
     * Creates the REST client for the given settings and obtains an access token.
     *
     * @param settings connection settings
     * @throws AuthenticationException if NiFi rejects the credentials or does not support username/password login
     * @throws IllegalStateException if the session is already open
     */
    public synchronized void open(ConnectionSettings settings) {
        if (client != null) {
            throw new IllegalStateException("NiFi session is already open");
        }
        client = buildClient(settings);
        Log.info("Authenticating...");
        try {
            accessToken = client.getAccessToken(settings.username(), settings.password());
        } catch (NiFiResponseException e) {
            throw new AuthenticationException(authenticationFailureMessage(e), e);
        }
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = null;
            throw new AuthenticationException("Authentication failed: NiFi returned an empty access token.", null);
        }
        Log.info("Authenticated!");
    }

    /**
     * Returns the client of the open session.
     *
     * @return authenticated NiFi client
     * @throws IllegalStateException if the session has not been opened
     */
    public synchronized NiFiClient client() {
        if (client == null) {
            throw new IllegalStateException("NiFi session is not open");
        }
        return client;
    }

    /**
     * Logs out (best effort) and releases the client. Safe to call more than once.
     */
    public synchronized void close() {
        try {
            if (client != null && accessToken != null) {
                client.logout();
            }
        } catch (RuntimeException e) {
            Log.warn("Unable to logout: " + e.getMessage());
        } finally {
            accessToken = null;
            if (client instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    Log.debug("Unable to close REST client", e);
                }
            }
            client = null;
        }
    }

    /**
     * Ensures the token is invalidated when the application is stopped early, e.g. by Ctrl+C.
     *
     * @param event shutdown event
     */
    void onShutdown(@Observes ShutdownEvent event) {
        close();
    }

    /**
     * Builds the REST client, applying the TLS settings.
     *
     * @param settings connection settings
     * @return new client
     */
    NiFiClient buildClient(ConnectionSettings settings) {
        QuarkusRestClientBuilder builder = QuarkusRestClientBuilder.newBuilder().baseUri(settings.baseUri()).register(new BearerTokenFilter());
        if (settings.insecure()) {
            Log.warn("TLS certificate and host name verification is DISABLED (--insecure). Do not use this against untrusted networks.");
            builder.trustAll(true).verifyHost(false);
        } else if (settings.trustStore() != null) {
            builder.trustStore(loadTrustStore(settings));
        }
        return builder.build(NiFiClient.class);
    }

    private static KeyStore loadTrustStore(ConnectionSettings settings) {
        char[] password = settings.trustStorePassword() == null ? null : settings.trustStorePassword().toCharArray();
        try {
            return KeyStore.getInstance(settings.trustStore().toFile(), password);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read trust store " + settings.trustStore() + ": " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Unable to load trust store " + settings.trustStore() + ": " + e.getMessage(), e);
        }
    }

    private static String authenticationFailureMessage(NiFiResponseException e) {
        return switch (e.getStatus()) {
            case 400, 401, 403 -> "Authentication failed: invalid username or password (HTTP " + e.getStatus() + ").";
            case 409 -> "Authentication failed: NiFi does not accept username/password login here (HTTP 409): " + e.getMessage();
            default -> "Authentication failed (HTTP " + e.getStatus() + "): " + e.getMessage();
        };
    }

    /**
     * Adds {@code Authorization: Bearer <token>} once a token is available.
     */
    private final class BearerTokenFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) {
            String token = accessToken;
            if (token != null) {
                requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            }
        }
    }
}
