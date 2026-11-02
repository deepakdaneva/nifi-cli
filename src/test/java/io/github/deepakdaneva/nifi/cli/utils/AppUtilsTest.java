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
package io.github.deepakdaneva.nifi.cli.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppUtilsTest {

    @ParameterizedTest
    @CsvSource({"https://nifi.example.com:8443, https://nifi.example.com:8443", "https://nifi.example.com:8443/nifi/, https://nifi.example.com:8443", "http://localhost:8080/nifi-api?x=1#frag, http://localhost:8080", "HTTPS://Nifi.Example.com, https://Nifi.Example.com", "'  https://host:9443  ', https://host:9443"})
    void reducesValidUrlsToSchemeHostAndPort(String input, String expected) throws URISyntaxException {
        assertEquals(URI.create(expected), AppUtils.getURI(input, false));
    }

    @Test
    void keepsUserInfo() throws URISyntaxException {
        assertEquals(URI.create("https://user@host:8443"), AppUtils.getURI("https://user@host:8443/path", false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "nifi.example.com:8443", "ftp://host", "https://", "https:///nifi", "not a url", "file:///etc/passwd"})
    void rejectsInvalidUrls(String input) throws URISyntaxException {
        assertNull(AppUtils.getURI(input, true));
        assertThrows(URISyntaxException.class, () -> AppUtils.getURI(input, false));
    }
}
