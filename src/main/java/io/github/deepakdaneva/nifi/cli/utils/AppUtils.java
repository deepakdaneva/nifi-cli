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
package io.github.deepakdaneva.nifi.cli.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * General purpose helper methods used by the CLI.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
public final class AppUtils {

    private AppUtils() {
    }

    /**
     * Parses a NiFi base URL and reduces it to {@code scheme://[userinfo@]host[:port]}.
     * <p>
     * Only {@code http} and {@code https} URLs with a host are accepted. Any path, query or fragment is discarded because the CLI always addresses
     * {@code /nifi-api} at the root of the given host.
     *
     * @param uriString to parse into URI instance.
     * @param ignoreException if {@code true}, {@code null} is returned instead of throwing for an invalid value.
     * @return URI instance of the provided URI string, or {@code null} if it is invalid and {@code ignoreException} is {@code true}.
     * @throws URISyntaxException if the value is not a valid http(s) URL and {@code ignoreException} is {@code false}
     */
    public static URI getURI(String uriString, boolean ignoreException) throws URISyntaxException {
        try {
            if (uriString == null || uriString.isBlank()) {
                throw new URISyntaxException(String.valueOf(uriString), "URL must not be empty");
            }
            URI uri = new URI(uriString.trim());
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new URISyntaxException(uriString, "URL scheme must be http or https");
            }
            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                throw new URISyntaxException(uriString, "URL must contain a host");
            }
            return new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            if (!ignoreException) {
                throw e;
            }
        }
        return null;
    }
}
