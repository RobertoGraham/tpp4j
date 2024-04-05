package io.github.robertograham.tpp4j;

import static java.util.function.Predicate.isEqual;
import static java.util.function.Predicate.not;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.TlsServerCredentials;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

final class ProviderServer {

    private static final int GO_PLUGIN_PROTOCOL_VERSION = 1;
    private static final int TERRAFORM_PLUGIN_PROTOCOL_VERSION = 6;

    private final Server server;

    ProviderServer(final EnvironmentVariables environmentVariables) throws IOException {
        final int minimumPortInclusive = environmentVariables.get("PLUGIN_MIN_PORT")
                .map(Integer::parseInt)
                .orElseThrow();
        final int maximumPortInclusive = environmentVariables.get("PLUGIN_MAX_PORT")
                .map(Integer::parseInt)
                .orElseThrow();
        final int port = findAvailablePort(minimumPortInclusive, maximumPortInclusive)
                .orElseThrow();
        final var clientCertificate = environmentVariables.get("PLUGIN_CLIENT_CERT")
                .orElseThrow();
        final var certificateChain = ClassLoader.getSystemResource("certificatechain.pem");
        try (final var certificateChainInputStream = certificateChain.openStream();
                final var privateKeyInputStream = ClassLoader.getSystemResourceAsStream("privatekey.pem");
                final var clientCertificateInputStream = IOUtils.toInputStream(clientCertificate, StandardCharsets.UTF_8)) {
            server = Grpc.newServerBuilderForPort(port, TlsServerCredentials.newBuilder()
                            .keyManager(certificateChainInputStream, privateKeyInputStream)
                            .trustManager(clientCertificateInputStream)
                            .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                            .build())
                    .addService(new DefaultProvider())
                    .build()
                    .start();
        }
        final var base64 = IOUtils.toString(certificateChain, StandardCharsets.UTF_8)
                .lines()
                .filter(not(isEqual("-----BEGIN CERTIFICATE-----")).and(not(isEqual("-----END CERTIFICATE-----"))))
                .collect(Collectors.joining());
        System.out.printf("%d|%d|tcp|localhost:%d|grpc|%s%n", GO_PLUGIN_PROTOCOL_VERSION, TERRAFORM_PLUGIN_PROTOCOL_VERSION, server.getPort(),
                base64);
    }

    void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    private Optional<Integer> findAvailablePort(final int minimumPortInclusive, final int maximumPortInclusive) {
        for (int port = minimumPortInclusive; port <= maximumPortInclusive; port++) {
            try (final var serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                return Optional.of(serverSocket.getLocalPort());
            } catch (final IOException exception) {
                System.err.printf("[WARN] Could not open socket for port %d%n", port);
            }
        }

        return Optional.empty();
    }
}
