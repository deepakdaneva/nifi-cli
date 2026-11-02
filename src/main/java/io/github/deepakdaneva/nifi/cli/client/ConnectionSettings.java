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

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Everything needed to connect and log in to a NiFi instance.
 * <p>
 * {@link #toString()} never includes the password or the trust store password.
 *
 * @param baseUri NiFi base URL ({@code scheme://host[:port]})
 * @param username NiFi user name
 * @param password password of the user
 * @param insecure if {@code true}, TLS certificates and host names are not verified
 * @param trustStore optional trust store (PKCS12 or JKS) used to verify the NiFi server certificate, may be {@code null}
 * @param trustStorePassword password of the trust store, may be {@code null}
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
public record ConnectionSettings(URI baseUri, String username, String password, boolean insecure, Path trustStore, String trustStorePassword) {

    /**
     * Validates the mandatory components.
     *
     * @param baseUri NiFi base URL
     * @param username NiFi user name
     * @param password password of the user
     * @param insecure whether TLS verification is disabled
     * @param trustStore optional trust store
     * @param trustStorePassword optional trust store password
     */
    public ConnectionSettings {
        Objects.requireNonNull(baseUri, "baseUri");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
    }

    /**
     * Returns the base URL without any user info, safe to show to users.
     *
     * @return {@code scheme://host[:port]}
     */
    public String displayUri() {
        return baseUri.getScheme() + "://" + baseUri.getHost() + (baseUri.getPort() >= 0 ? ":" + baseUri.getPort() : "");
    }

    /**
     * Returns a description of these settings with all secrets masked.
     *
     * @return description without secrets
     */
    @Override
    public String toString() {
        return "ConnectionSettings[baseUri=" + displayUri() + ", username=" + username + ", password=****, insecure=" + insecure + ", trustStore=" + trustStore + "]";
    }
}
