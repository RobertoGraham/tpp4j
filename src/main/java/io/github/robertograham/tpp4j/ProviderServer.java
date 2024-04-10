package io.github.robertograham.tpp4j;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.ServerTransportFilter;
import io.grpc.TlsServerCredentials;
import java.io.IOException;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.util.io.pem.PemWriter;

final class ProviderServer {

    private static final int GO_PLUGIN_PROTOCOL_VERSION = 1;
    private static final int TERRAFORM_PLUGIN_PROTOCOL_VERSION = 6;

    private final EnvironmentVariables environmentVariables;
    private Server server;

    ProviderServer(final EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    void start() throws IOException, GeneralSecurityException {
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
        final var privateCredential = PrivateCredentialHelper.generate();
        try (final var stringWriter = new StringWriter();
                final var pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new MiscPEMGenerator(privateCredential.certificate()));
            pemWriter.flush();
            stringWriter.flush();
            final var caCertificate = stringWriter.toString();
            pemWriter.writeObject(new PKCS8Generator(privateCredential.privateKey(), null));
            pemWriter.flush();
            stringWriter.flush();
            final var privateKey = stringWriter.toString();
            try (final var certificateChainInputStream = IOUtils.toInputStream(caCertificate, StandardCharsets.UTF_8);
                    final var privateKeyInputStream = IOUtils.toInputStream(privateKey, StandardCharsets.UTF_8);
                    final var clientCertificateInputStream = IOUtils.toInputStream(clientCertificate, StandardCharsets.UTF_8)) {
                server = Grpc.newServerBuilderForPort(port, TlsServerCredentials.newBuilder()
                                .keyManager(certificateChainInputStream, privateKeyInputStream)
                                .trustManager(clientCertificateInputStream)
                                .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                                .build())
                        .addService(new DefaultProvider())
                        .addTransportFilter(new ServerTransportFilter() {
                            @Override
                            public void transportTerminated(final Attributes attributes) {
                                stop();
                            }
                        })
                        .build()
                        .start();
            }
        }
        System.out.printf("%d|%d|tcp|localhost:%d|grpc|%s%n", GO_PLUGIN_PROTOCOL_VERSION, TERRAFORM_PLUGIN_PROTOCOL_VERSION, server.getPort(),
                Base64.getEncoder()
                        .withoutPadding()
                        .encodeToString(privateCredential.certificate()
                                .getEncoded()));
    }

    void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private void stop() {
        if (server != null) {
            System.err.println("[INFO] Shutting down provider server");
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
