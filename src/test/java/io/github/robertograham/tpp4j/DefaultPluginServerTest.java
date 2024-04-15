package io.github.robertograham.tpp4j;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hashicorp.goplugin.Empty;
import com.hashicorp.goplugin.GRPCControllerGrpc;
import com.hashicorp.goplugin.GRPCControllerGrpc.GRPCControllerBlockingStub;
import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;
import io.terraform.tfplugin6.GetProviderSchema.Request;
import io.terraform.tfplugin6.ProviderGrpc;
import io.terraform.tfplugin6.ProviderGrpc.ProviderBlockingStub;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DefaultPluginServerTest {

    @Mock
    private EnvironmentVariables environmentVariables;
    private DefaultPluginServer defaultProviderServer;
    private ProviderBlockingStub providerBlockingStub;
    private GRPCControllerBlockingStub grpcControllerBlockingStub;

    @BeforeEach
    void beforeEach() {
        final var port = findAvailablePort();
        when(environmentVariables.get("PLUGIN_MIN_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));
        when(environmentVariables.get("PLUGIN_MAX_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));
        final var clientPrivateCredential = PrivateCredential.newPrivateCredential();
        final var clientCertificate = clientPrivateCredential.rfc7468EncodedX509Certificate();
        when(environmentVariables.get("PLUGIN_CLIENT_CERT"))
                .thenReturn(Optional.of(clientCertificate));
        final var serverPrivateCredential = PrivateCredential.newPrivateCredential();
        final var clientCertificateInputStream = new ByteArrayInputStream(clientCertificate.getBytes(StandardCharsets.UTF_8));
        final var privateKeyInputStream = new ByteArrayInputStream(clientPrivateCredential.rfc7468EncodedPkcs8PrivateKeyInfo()
                .getBytes(StandardCharsets.UTF_8));
        final var serverCertificateInputStream = new ByteArrayInputStream(serverPrivateCredential.rfc7468EncodedX509Certificate()
                .getBytes(StandardCharsets.UTF_8));
        try {
            final var managedChannel = Grpc.newChannelBuilderForAddress("localhost", port, TlsChannelCredentials.newBuilder()
                            .keyManager(clientCertificateInputStream, privateKeyInputStream)
                            .trustManager(serverCertificateInputStream)
                            .build())
                    .build();
            providerBlockingStub = ProviderGrpc.newBlockingStub(managedChannel);
            grpcControllerBlockingStub = GRPCControllerGrpc.newBlockingStub(managedChannel);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
        defaultProviderServer = new DefaultPluginServer(() -> environmentVariables, () -> serverPrivateCredential);
        defaultProviderServer.run();
    }

    @AfterEach
    void afterEach() {
        grpcControllerBlockingStub.shutdown(Empty.newBuilder()
                .build());
        defaultProviderServer.awaitShutdown();
    }

    @Test
    void getProviderSchema() {
        final var response = providerBlockingStub.getProviderSchema(Request.newBuilder()
                .build());

        assertTrue(response.hasProvider());
    }

    private int findAvailablePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
