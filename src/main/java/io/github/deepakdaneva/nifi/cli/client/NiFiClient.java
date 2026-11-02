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
package io.github.deepakdaneva.nifi.cli.client;

import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessGroupFlowEntity;

/**
 * Typed client for the subset of the NiFi REST API ({@code /nifi-api}) used by the CLI.
 * <p>
 * Instances are created by {@link NiFiSession}, which also adds the bearer token to every request after login.
 *
 * @author Deepak Kumar Jangir
 * @version 2
 * @since 1
 */
@Path("/nifi-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface NiFiClient {

    /**
     * Maximum number of characters of an error response body kept in exception messages.
     */
    int MAX_ERROR_BODY_LENGTH = 512;

    /**
     * Converts every HTTP error response into a {@link NiFiResponseException}.
     *
     * @param response error response received from NiFi
     * @return exception carrying the status and (truncated) response body
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        String body = null;
        try {
            if (response.hasEntity()) {
                body = response.readEntity(String.class);
            }
        } catch (RuntimeException ignored) {
            // body is optional; the status alone is still meaningful
        }
        if (body == null || body.isBlank()) {
            body = response.getStatusInfo().getReasonPhrase();
        } else if (body.length() > MAX_ERROR_BODY_LENGTH) {
            body = body.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
        }
        return new NiFiResponseException(body.strip(), response.getStatus());
    }

    /**
     * Exchanges username and password for an access token ({@code POST /access/token}).
     *
     * @param username NiFi user name
     * @param password password of the user
     * @return access token (JWT) to send as bearer token on subsequent requests
     */
    @POST
    @Path("/access/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    String getAccessToken(@FormParam("username") String username, @FormParam("password") String password);

    /**
     * Invalidates the current access token ({@code DELETE /access/logout}).
     */
    @DELETE
    @Path("/access/logout")
    @Produces(MediaType.WILDCARD)
    void logout();

    /**
     * Fetches the flow (direct children) of a process group ({@code GET /flow/process-groups/{id}}).
     *
     * @param id process group id, or {@code root} for the root process group
     * @return process group flow entity
     */
    @GET
    @Path("/flow/process-groups/{id}")
    ProcessGroupFlowEntity getFlowProcessGroup(@PathParam("id") String id);

    /**
     * Updates a process group ({@code PUT /process-groups/{id}}).
     *
     * @param id id of the process group to update
     * @param pgEntity entity holding the current revision and the fields to change
     */
    @PUT
    @Path("/process-groups/{id}")
    void updateProcessGroup(@PathParam("id") String id, ProcessGroupEntity pgEntity);
}
