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

import io.github.deepakdaneva.nifi.cli.FakeNiFiServer.Recorded;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real CLI against a fake NiFi REST API.
 */
@QuarkusMainTest
class NiFiCliMainTest {

    private static FakeNiFiServer nifi;

    @BeforeAll
    static void startServer() throws Exception {
        nifi = new FakeNiFiServer();
    }

    @AfterAll
    static void stopServer() {
        nifi.close();
    }

    @BeforeEach
    void resetServer() {
        nifi.reset();
    }

    private String[] args(String... extra) {
        String[] common = {"-l", nifi.baseUrl(), "-u", FakeNiFiServer.USERNAME, "-p", FakeNiFiServer.PASSWORD};
        String[] all = new String[common.length + extra.length];
        System.arraycopy(common, 0, all, 0, common.length);
        System.arraycopy(extra, 0, all, common.length, extra.length);
        return all;
    }

    @Test
    void alignsProcessGroupsAndLogsOut(QuarkusMainLauncher launcher) {
        nifi.flow("root", flow("root-id", group("pg-2", "Second", 7, 500, 500), group("pg-1", "First", 3, 900, 100)));
        nifi.flow("pg-1", flow("pg-1", group("pg-1-1", "Nested", 1, 50, 50)));
        nifi.flow("pg-2", flowWithProcessor("pg-2", group("pg-2-1", "Untouched", 1, 50, 50)));
        nifi.flow("pg-1-1", flow("pg-1-1"));

        LaunchResult result = launcher.launch(args("align", "-d", "-1", "-c", "1"));

        assertEquals(0, result.exitCode(), result.getOutput() + result.getErrorOutput());
        List<Recorded> requests = nifi.requests();
        assertEquals("POST /nifi-api/access/token", line(requests.get(0)));
        assertEquals(null, requests.get(0).authorization());
        assertEquals("DELETE /nifi-api/access/logout", line(requests.get(requests.size() - 1)));

        List<Recorded> puts = requests.stream().filter(r -> r.method().equals("PUT")).toList();
        assertEquals(List.of("PUT /nifi-api/process-groups/pg-1-1", "PUT /nifi-api/process-groups/pg-1", "PUT /nifi-api/process-groups/pg-2"), puts.stream().map(NiFiCliMainTest::line).toList());
        assertPut(puts.get(1), "pg-1", 3, 0, 0);
        assertPut(puts.get(2), "pg-2", 7, 0, 186);
        assertTrue(requests.stream().skip(1).allMatch(r -> ("Bearer " + FakeNiFiServer.TOKEN).equals(r.authorization())));
        assertFalse(result.getOutput().contains(FakeNiFiServer.PASSWORD), "password must never be printed");
        assertFalse(result.getOutput().contains(FakeNiFiServer.TOKEN), "token must never be printed");
    }

    @Test
    void depthOneOnlyAlignsDirectChildren(QuarkusMainLauncher launcher) {
        nifi.flow("custom", flow("custom", group("pg-a", "a", 1, 0, 0), group("pg-b", "b", 1, 0, 0)));
        LaunchResult result = launcher.launch(args("align", "--depth", "1", "--rootpgid", "custom", "--columns", "4"));
        assertEquals(0, result.exitCode(), result.getOutput() + result.getErrorOutput());
        assertEquals(1, nifi.requests().stream().filter(r -> r.method().equals("GET")).count());
        Recorded second = nifi.requests().stream().filter(r -> r.method().equals("PUT")).toList().get(1);
        assertPut(second, "pg-b", 1, 394, 0);
    }

    @Test
    void invalidCredentialsExitWithAuthenticationCode(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("-l", nifi.baseUrl(), "-u", "admin", "-p", "wrong", "align");
        assertEquals(3, result.exitCode());
        assertTrue(result.getOutput().contains("invalid username or password"), result.getOutput());
        assertTrue(nifi.requests().stream().noneMatch(r -> r.path().endsWith("/logout")), "no logout without a token");
    }

    @Test
    void nifiErrorsExitWithSoftwareCodeAndStillLogOut(QuarkusMainLauncher launcher) {
        nifi.flow("root", flow("root-id", group("pg-1", "First", 3, 0, 0)));
        nifi.updateStatus(409);
        LaunchResult result = launcher.launch(args("align", "-d", "1"));
        assertEquals(1, result.exitCode());
        assertTrue(result.getOutput().contains("HTTP 409"), result.getOutput());
        assertEquals("DELETE /nifi-api/access/logout", line(nifi.requests().get(nifi.requests().size() - 1)));
    }

    @Test
    void unknownProcessGroupExitsWithSoftwareCode(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args("align", "-r", "missing"));
        assertEquals(1, result.exitCode());
        assertTrue(result.getOutput().contains("HTTP 404"), result.getOutput());
    }

    @Test
    void unreachableNiFiExitsWithSoftwareCode(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("-l", "http://localhost:1", "-u", "a", "-p", "b", "align");
        assertEquals(1, result.exitCode());
    }

    @Test
    void missingSubcommandIsUsageError(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args());
        assertEquals(2, result.exitCode());
        assertTrue(nifi.requests().isEmpty(), "must not contact NiFi");
    }

    @Test
    void invalidLocationIsUsageError(QuarkusMainLauncher launcher) {
        assertEquals(2, launcher.launch("-l", "nifi.example.com:8443", "-u", "a", "-p", "b", "align").exitCode());
    }

    @Test
    void unknownOptionIsUsageError(QuarkusMainLauncher launcher) {
        assertEquals(2, launcher.launch(args("align", "--colums", "3")).exitCode());
        assertTrue(nifi.requests().isEmpty(), "must not contact NiFi");
    }

    @Test
    void invalidColumnsIsUsageError(QuarkusMainLauncher launcher) {
        assertEquals(2, launcher.launch(args("align", "-c", "0")).exitCode());
        assertEquals(2, launcher.launch(args("align", "-c", "x")).exitCode());
        assertTrue(nifi.requests().isEmpty(), "must not contact NiFi");
    }

    @Test
    void versionAndHelpDoNotContactNiFi(QuarkusMainLauncher launcher) {
        LaunchResult version = launcher.launch("--version");
        assertEquals(0, version.exitCode());
        assertTrue(version.getOutput().startsWith("nifi-cli "), version.getOutput());
        assertEquals(0, launcher.launch("--help").exitCode());
        LaunchResult alignHelp = launcher.launch("help", "align");
        assertEquals(0, alignHelp.exitCode());
        assertTrue(alignHelp.getOutput().contains("--rootpgid"), alignHelp.getOutput());
        assertTrue(nifi.requests().isEmpty());
    }

    private static String line(Recorded r) {
        return r.method() + " " + r.path();
    }

    private static void assertPut(Recorded put, String id, long version, double x, double y) {
        try (JsonReader reader = Json.createReader(new StringReader(put.body()))) {
            JsonObject body = reader.readObject();
            assertEquals(version, body.getJsonObject("revision").getJsonNumber("version").longValue(), put.body());
            JsonObject component = body.getJsonObject("component");
            assertEquals(id, component.getString("id"));
            assertEquals(x, component.getJsonObject("position").getJsonNumber("x").doubleValue());
            assertEquals(y, component.getJsonObject("position").getJsonNumber("y").doubleValue());
            assertFalse(component.containsKey("name"), "only id and position may be sent: " + put.body());
        }
    }

    private static String group(String id, String name, long version, double x, double y) {
        return """
                {"revision":{"version":%d,"lastModifier":"admin"},"id":"%s","uri":"http://localhost/nifi-api/process-groups/%s",
                 "position":{"x":%s,"y":%s},"permissions":{"canRead":true,"canWrite":true},
                 "component":{"id":"%s","name":"%s","position":{"x":%s,"y":%s},"comments":"","runningCount":0,"stoppedCount":1},
                 "status":{"id":"%s","name":"%s","statsLastRefreshed":"12:34:56 UTC",
                   "aggregateSnapshot":{"id":"%s","name":"%s","flowFilesIn":0,"bytesIn":0,"input":"0 (0 bytes)"}},
                 "runningCount":0,"stoppedCount":1,"futureNiFiField":{"nested":[1,2,3]}}
                """.formatted(version, id, id, x, y, id, name, x, y, id, name, id, name);
    }

    private static String flow(String id, String... groups) {
        return flowJson(id, "[]", groups);
    }

    private static String flowWithProcessor(String id, String... groups) {
        String processor = """
                [{"revision":{"version":0},"id":"proc-1","component":{"id":"proc-1","name":"LogAttribute","type":"org.apache.nifi.processors.standard.LogAttribute",
                  "config":{"properties":{"Log Level":"info"},"schedulingPeriod":"0 sec"}},"status":{"statsLastRefreshed":"01:02:03 UTC"}}]
                """;
        return flowJson(id, processor, groups);
    }

    private static String flowJson(String id, String processors, String... groups) {
        return """
                {"permissions":{"canRead":true,"canWrite":true},
                 "processGroupFlow":{"id":"%s","uri":"http://localhost/nifi-api/flow/process-groups/%s",
                  "breadcrumb":{"id":"%s","permissions":{"canRead":true,"canWrite":true},"breadcrumb":{"id":"%s","name":"NiFi Flow"}},
                  "flow":{"processGroups":[%s],"remoteProcessGroups":[],"processors":%s,"inputPorts":[],"outputPorts":[],
                          "connections":[],"labels":[],"funnels":[]},
                  "lastRefreshed":"10:11:12 UTC"}}
                """.formatted(id, id, id, id, String.join(",", groups), processors);
    }
}
