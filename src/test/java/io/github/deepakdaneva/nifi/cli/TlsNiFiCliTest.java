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

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that TLS certificates and host names are checked by default and that {@code --insecure} / {@code --truststore} behave as documented.
 */
@QuarkusMainTest
class TlsNiFiCliTest {

    private static final String STORE_PASSWORD = "changeit";

    private static Path dir;
    private static FakeNiFiServer localhostServer;
    private static FakeNiFiServer wrongHostServer;
    private static Path localhostTrust;
    private static Path wrongHostTrust;

    @BeforeAll
    static void startServers() throws Exception {
        dir = Files.createTempDirectory("nifi-cli-tls");
        localhostServer = new FakeNiFiServer(serverContext(createKeyStore("localhost", "SAN=dns:localhost,ip:127.0.0.1")));
        localhostTrust = createTrustStore("localhost");
        wrongHostServer = new FakeNiFiServer(serverContext(createKeyStore("wrong", "SAN=dns:wrong.example.com")));
        wrongHostTrust = createTrustStore("wrong");
        for (FakeNiFiServer server : List.of(localhostServer, wrongHostServer)) {
            server.flow("root", "{\"processGroupFlow\":{\"id\":\"root\",\"flow\":{\"processGroups\":[]}}}");
        }
    }

    @AfterAll
    static void stopServers() throws IOException {
        localhostServer.close();
        wrongHostServer.close();
        try (var files = Files.walk(dir)) {
            files.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> p.toFile().delete());
        }
    }

    private static String[] args(FakeNiFiServer server, String... extra) {
        List<String> all = new ArrayList<>(List.of("-l", server.baseUrl(), "-u", FakeNiFiServer.USERNAME, "-p", FakeNiFiServer.PASSWORD));
        all.addAll(List.of(extra));
        all.add("align");
        return all.toArray(String[]::new);
    }

    @Test
    void rejectsUntrustedCertificateByDefault(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args(localhostServer));
        assertEquals(1, result.exitCode(), result.getOutput());
    }

    @Test
    void acceptsCertificateFromTrustStore(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args(localhostServer, "--truststore", localhostTrust.toString(), "--truststore-password", STORE_PASSWORD));
        assertEquals(0, result.exitCode(), result.getOutput());
    }

    @Test
    void verifiesHostNameEvenWithTrustedCertificate(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args(wrongHostServer, "--truststore", wrongHostTrust.toString(), "--truststore-password", STORE_PASSWORD));
        assertEquals(1, result.exitCode(), result.getOutput());
    }

    @Test
    void insecureSkipsVerificationWithWarning(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch(args(wrongHostServer, "--insecure"));
        assertEquals(0, result.exitCode(), result.getOutput());
        assertTrue(result.getOutput().contains("verification is DISABLED"), result.getOutput());
    }

    private static Path createKeyStore(String alias, String san) throws Exception {
        Path keyStore = dir.resolve(alias + "-server.p12");
        keytool("-genkeypair", "-alias", alias, "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=" + alias, "-ext", san, "-validity", "2", "-storetype", "PKCS12", "-keystore", keyStore.toString(), "-storepass", STORE_PASSWORD, "-keypass", STORE_PASSWORD);
        return keyStore;
    }

    private static Path createTrustStore(String alias) throws Exception {
        Path cert = dir.resolve(alias + ".pem");
        Path trustStore = dir.resolve(alias + "-trust.p12");
        keytool("-exportcert", "-rfc", "-alias", alias, "-keystore", dir.resolve(alias + "-server.p12").toString(), "-storepass", STORE_PASSWORD, "-file", cert.toString());
        keytool("-importcert", "-noprompt", "-alias", alias, "-file", cert.toString(), "-storetype", "PKCS12", "-keystore", trustStore.toString(), "-storepass", STORE_PASSWORD);
        return trustStore;
    }

    private static SSLContext serverContext(Path keyStorePath) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keyStorePath)) {
            keyStore.load(in, STORE_PASSWORD.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, STORE_PASSWORD.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, null);
        return context;
    }

    private static void keytool(String... args) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", windows ? "keytool.exe" : "keytool").toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
    }
}
