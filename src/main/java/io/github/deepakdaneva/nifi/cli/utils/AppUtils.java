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

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
public class AppUtils {

    /**
     * This method parses the URI string and returns null if any exception is raised and ignoreException is true.
     * 
     * @param uriString to parse into URI instance.
     * @param ignoreException whether to ignore throwing exception or not.
     * @return URI instance of the provided URI string.
     * @throws URISyntaxException if string is syntactically invalid
     */
    public static URI getURI(String uriString, boolean ignoreException) throws URISyntaxException {
        try {
            URI uri = new URI(uriString);
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        } catch (Exception e) {
            if (!ignoreException) throw e;
        }
        return null;
    }
}
