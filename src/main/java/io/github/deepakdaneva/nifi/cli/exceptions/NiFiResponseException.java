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
package io.github.deepakdaneva.nifi.cli.exceptions;

import jakarta.ws.rs.core.Response;
import lombok.Getter;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@Getter
public class NiFiResponseException extends RuntimeException {
    /**
     * Status code
     */
    final int status;
    /**
     * Response received from NiFi REST API call
     */
    final Response response;

    /**
     * Create instance with the provided message, status and response received from NiFi
     * 
     * @param message of the exception
     * @param status code of the response
     * @param response received from NiFi
     */
    public NiFiResponseException(String message, int status, Response response) {
        super(message);
        this.status = status;
        this.response = response;
    }
}
