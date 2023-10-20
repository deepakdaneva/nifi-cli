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
package io.github.deepakdaneva.nifi.cli.services;

import io.github.deepakdaneva.nifi.cli.configs.AppRestClientConfig;
import io.github.deepakdaneva.nifi.cli.exceptions.NiFiResponseException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.enterprise.context.Dependent;
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
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

import java.lang.reflect.Method;

/**
 * @author Deepak Kumar Jangir
 * @version 1
 * @since 1
 */
@Dependent
@Path("/nifi-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = NiFiService.AUTHORIZATION_HEADER_KEY, value = "{authorizationHeaderValue}")
public interface NiFiService {

    /**
     * Authorization key for NiFi REST APIs
     */
    String AUTHORIZATION_HEADER_KEY = "Authorization";

    /**
     * Map the response to proper exception
     * 
     * @param response received from the NiFi REST API
     * @param invokedMethod method invoked in {@link io.github.deepakdaneva.nifi.cli.services.NiFiService}
     * @return runtime exception
     */
    @ClientExceptionMapper
    static RuntimeException toException(Response response, Method invokedMethod) {
        String invokedMethodName = invokedMethod.getName();
        if ("getAccessToken".equals(invokedMethodName)) {
            if (response.getStatus() == 400) {
                return new NiFiResponseException("Unauthorized!", HttpResponseStatus.UNAUTHORIZED.code(), response);
            }
        }

        if (response.getStatus() >= 400) {
            return new NiFiResponseException(String.valueOf(response.readEntity(String.class)), response.getStatus(), response);
        }

        // Handle other methods or return null if no specific handling is required
        return null;
    }

    /**
     * Provides the header value to use for authorization header key
     * 
     * @return authorization header value to use directly in header
     */
    default String authorizationHeaderValue() {
        return AppRestClientConfig.accessToken != null ? "Bearer " + AppRestClientConfig.accessToken : "";
    }

    /**
     * Uses username and password to generate access token for other subsequent REST API calls.
     * 
     * @param username to access NiFi
     * @param password of the username
     * @return generated access token
     */
    @POST
    @Path("/access/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @ClientHeaderParam(name = NiFiService.AUTHORIZATION_HEADER_KEY, value = "")
    String getAccessToken(@FormParam("username") String username, @FormParam("password") String password);

    /**
     * Logs out from the NiFi
     * 
     * @throws RuntimeException in case something fails
     */
    @DELETE
    @Path("/access/logout")
    void logout() throws RuntimeException;

    /**
     * Provides {@link org.apache.nifi.web.api.entity.ProcessGroupFlowEntity} of the provided process group by id
     * 
     * @param id of the process group
     * @return process group flow entity
     */
    @GET
    @Path("/flow/process-groups/{id}")
    ProcessGroupFlowEntity getFlowProcessGroup(@PathParam("id") String id);

    /**
     * Updates process group by id
     * 
     * @param id of the process group to update
     * @param pgEntity updated process group entity of the process group to update in the NiFi
     */
    @PUT
    @Path("/process-groups/{id}")
    void updateProcessGroup(@PathParam("id") String id, ProcessGroupEntity pgEntity);
}
