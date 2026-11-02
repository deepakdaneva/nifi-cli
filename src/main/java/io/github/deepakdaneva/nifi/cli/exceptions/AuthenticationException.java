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
package io.github.deepakdaneva.nifi.cli.exceptions;

/**
 * Signals that NiFi rejected the supplied credentials or does not allow username/password login.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 2
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an authentication failure.
     *
     * @param message description of the failure; must not contain credentials
     * @param cause underlying error, may be {@code null}
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
