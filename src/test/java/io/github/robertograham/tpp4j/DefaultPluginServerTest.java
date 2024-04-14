package io.github.robertograham.tpp4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.hashicorp.goplugin.Empty;
import com.hashicorp.goplugin.GRPCControllerGrpc;
import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;
import io.terraform.tfplugin6.GetProviderSchema.Request;
import io.terraform.tfplugin6.ProviderGrpc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class DefaultPluginServerTest {

    private final EnvironmentVariables environmentVariables;
    private final DefaultPluginServer defaultProviderServer;

    DefaultPluginServerTest(@Mock final EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        defaultProviderServer = new DefaultPluginServer(() -> this.environmentVariables, PrivateCredential::newPrivateCredential);
    }

    @Test
    void getProviderSchema() throws IOException {
        final var port = findAvailablePort();

        when(environmentVariables.get("PLUGIN_MIN_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));
        when(environmentVariables.get("PLUGIN_MAX_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));

        final var privateCredential = PrivateCredential.newPrivateCredential();
        final var clientCertificate = privateCredential.rfc7468EncodedX509Certificate();

        when(environmentVariables.get("PLUGIN_CLIENT_CERT"))
                .thenReturn(Optional.of(clientCertificate));

        final var serverCertificateHolder = startServerAndExtractCertificate();

        final var clientCertificateInputStream = new ByteArrayInputStream(clientCertificate.getBytes(StandardCharsets.UTF_8));
        final var privateKey = privateCredential.rfc7468EncodedPkcs8PrivateKeyInfo();
        final var privateKeyInputStream = new ByteArrayInputStream(privateKey.getBytes(StandardCharsets.UTF_8));
        final var serverCertificate = pemEncodeCertificate(serverCertificateHolder);
        final var serverCertificateInputStream = new ByteArrayInputStream(serverCertificate.getBytes(StandardCharsets.UTF_8));
        final var managedChannel = Grpc.newChannelBuilderForAddress("localhost", port, TlsChannelCredentials.newBuilder()
                        .keyManager(clientCertificateInputStream, privateKeyInputStream)
                        .trustManager(serverCertificateInputStream)
                        .build())
                .build();
        final var providerClient = ProviderGrpc.newBlockingStub(managedChannel);

        final var response = providerClient.getProviderSchema(Request.newBuilder()
                .build());

        assertTrue(response.hasProvider());
        assertEquals(Empty.newBuilder()
                .build(), GRPCControllerGrpc.newBlockingStub(managedChannel)
                .shutdown(Empty.newBuilder()
                        .build()));

        defaultProviderServer.awaitShutdown();
    }

    private int findAvailablePort() throws IOException {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
            serverSocket.setReuseAddress(true);
            return serverSocket.getLocalPort();
        }
    }

    private String pemEncodeCertificate(final X509CertificateHolder x509CertificateHolder) throws IOException {
        try (final var stringWriter = new StringWriter();
                final var pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new MiscPEMGenerator(x509CertificateHolder));
            pemWriter.flush();
            stringWriter.flush();

            return stringWriter.toString();
        }
    }

    private X509CertificateHolder startServerAndExtractCertificate() throws IOException {
        final var currentPrintStream = System.out;
        try (final var outputStream = new ByteArrayOutputStream();
                final var printStream = new PrintStream(outputStream)) {
            System.setOut(printStream);
            defaultProviderServer.run();
            System.setOut(currentPrintStream);
            final var protocolNegotiationLine = outputStream.toString(StandardCharsets.UTF_8)
                    .trim();

            return new X509CertificateHolder(Base64.getDecoder()
                    .decode(protocolNegotiationLine.substring(protocolNegotiationLine.lastIndexOf("|") + 1)));
        }
    }
}
