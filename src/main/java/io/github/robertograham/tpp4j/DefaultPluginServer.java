package io.github.robertograham.tpp4j;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.TlsServerCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

final class DefaultPluginServer implements PluginServer {

    private static final int GO_PLUGIN_PROTOCOL_VERSION = 1;
    private static final int TERRAFORM_PLUGIN_PROTOCOL_VERSION = 6;

    private final Supplier<EnvironmentVariables> environmentVariablesFactory;
    private final Supplier<PrivateCredential> privateCredentialFactory;
    private Server server;

    DefaultPluginServer(final Supplier<EnvironmentVariables> environmentVariablesFactory,
            final Supplier<PrivateCredential> privateCredentialFactory) {
        this.environmentVariablesFactory = environmentVariablesFactory;
        this.privateCredentialFactory = privateCredentialFactory;
    }

    @Override
    public void run() {
        final var environmentVariables = environmentVariablesFactory.get();
        final int minimumPortInclusive = environmentVariables.get("PLUGIN_MIN_PORT")
                .map(Integer::parseInt)
                .orElseThrow();
        final int maximumPortInclusive = environmentVariables.get("PLUGIN_MAX_PORT")
                .map(Integer::parseInt)
                .orElseThrow();
        final int port = findAvailablePort(minimumPortInclusive, maximumPortInclusive)
                .orElseThrow();
        final var privateCredential = privateCredentialFactory.get();
        final var certificateChainInputStream = new ByteArrayInputStream(
                privateCredential.rfc7468EncodedX509Certificate().getBytes(StandardCharsets.UTF_8));
        final var privateKeyInputStream = new ByteArrayInputStream(
                privateCredential.rfc7468EncodedPkcs8PrivateKeyInfo().getBytes(StandardCharsets.UTF_8));
        final var clientCertificateInputStream = environmentVariables.get("PLUGIN_CLIENT_CERT")
                .map(StandardCharsets.UTF_8::encode)
                .map(ByteBuffer::array)
                .map(ByteArrayInputStream::new)
                .orElseThrow();
        try {
            server = Grpc.newServerBuilderForPort(port, TlsServerCredentials.newBuilder()
                            .keyManager(certificateChainInputStream, privateKeyInputStream)
                            .trustManager(clientCertificateInputStream)
                            .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                            .build())
                    .addService(new DefaultProvider())
                    .addService(new DefaultGrpcController(this::shutdown))
                    .build();
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to install mTLS credentials.", exception);
        }
        try {
            server.start();
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to start gRPC server.", exception);
        }
        System.out.printf("%d|%d|tcp|localhost:%d|grpc|%s%n", GO_PLUGIN_PROTOCOL_VERSION, TERRAFORM_PLUGIN_PROTOCOL_VERSION, server.getPort(),
                privateCredential.base64EncodedX509CertificateWithoutPadding());
    }

    @Override
    public void awaitShutdown() {
        if (server != null) {
            try {
                server.awaitTermination();
            } catch (final InterruptedException exception) {
                Thread.currentThread()
                        .interrupt();
            }
        }
    }

    private void shutdown() {
        if (server != null) {
            server.shutdown();
        }
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
