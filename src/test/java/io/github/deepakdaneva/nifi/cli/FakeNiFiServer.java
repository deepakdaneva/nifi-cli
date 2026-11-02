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
package io.github.deepakdaneva.nifi.cli;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLContext;

/**
 * Minimal NiFi REST API stand-in built on the JDK HTTP server, so the tests need no extra dependency.
 */
final class FakeNiFiServer implements AutoCloseable {

    static final String USERNAME = "admin";
    static final String PASSWORD = "s3cret-Pa55";
    static final String TOKEN = "eyJ.test.token";

    /**
     * A request received by the server.
     */
    record Recorded(String method, String path, String authorization, String body) {
    }

    private final HttpServer server;
    private final Map<String, String> flows = new ConcurrentHashMap<>();
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private volatile int updateStatus = 200;

    FakeNiFiServer() throws IOException {
        this(null);
    }

    /**
     * @param sslContext if not {@code null}, the server speaks HTTPS with this context
     */
    FakeNiFiServer(SSLContext sslContext) throws IOException {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        if (sslContext == null) {
            server = HttpServer.create(address, 0);
        } else {
            HttpsServer https = HttpsServer.create(address, 0);
            https.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            server = https;
        }
        server.createContext("/nifi-api/", this::handle);
        server.start();
    }

    String baseUrl() {
        return (server instanceof HttpsServer ? "https" : "http") + "://localhost:" + server.getAddress().getPort();
    }

    void flow(String id, String json) {
        flows.put(id, json);
    }

    void updateStatus(int status) {
        updateStatus = status;
    }

    List<Recorded> requests() {
        return List.copyOf(requests);
    }

    void reset() {
        flows.clear();
        requests.clear();
        updateStatus = 200;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        requests.add(new Recorded(method, path, authorization, body));

        if ("POST".equals(method) && path.equals("/nifi-api/access/token")) {
            Map<String, String> form = parseForm(body);
            if (USERNAME.equals(form.get("username")) && PASSWORD.equals(form.get("password"))) {
                respond(exchange, 201, "text/plain", TOKEN);
            } else {
                respond(exchange, 400, "text/plain", "The supplied username and password are not valid.");
            }
            return;
        }
        if (!("Bearer " + TOKEN).equals(authorization)) {
            respond(exchange, 401, "text/plain", "Authentication is required.");
            return;
        }
        if ("DELETE".equals(method) && path.equals("/nifi-api/access/logout")) {
            respond(exchange, 200, "text/plain", "");
        } else if ("GET".equals(method) && path.startsWith("/nifi-api/flow/process-groups/")) {
            String json = flows.get(path.substring("/nifi-api/flow/process-groups/".length()));
            if (json == null) {
                respond(exchange, 404, "text/plain", "Unable to find process group.");
            } else {
                respond(exchange, 200, "application/json", json);
            }
        } else if ("PUT".equals(method) && path.startsWith("/nifi-api/process-groups/")) {
            respond(exchange, updateStatus, updateStatus == 200 ? "application/json" : "text/plain", updateStatus == 200 ? body : "Revision mismatch.");
        } else {
            respond(exchange, 404, "text/plain", "Not found");
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> form = new ConcurrentHashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                form.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
