package io.github.robertograham.tpp4j;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.grpc.Grpc;
import io.grpc.TlsChannelCredentials;
import io.terraform.tfplugin6.GetProviderSchema.Request;
import io.terraform.tfplugin6.ProviderGrpc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class ProviderServerTest {

    private final EnvironmentVariables environmentVariables;
    private final ProviderServer providerServer;

    ProviderServerTest(@Mock final EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        providerServer = new ProviderServer(this.environmentVariables);
    }

    @Test
    void getProviderSchema() throws IOException, GeneralSecurityException, InterruptedException {
        final var port = findAvailablePort();
        final var privateCredential = PrivateCredentialHelper.generate();
        final var clientCertificate = pemEncodeCertificate(privateCredential.certificate());
        final var privateKey = pemEncodePrivateKey(privateCredential.privateKey());

        when(environmentVariables.get("PLUGIN_MIN_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));
        when(environmentVariables.get("PLUGIN_MAX_PORT"))
                .thenReturn(Optional.of(String.valueOf(port)));
        when(environmentVariables.get("PLUGIN_CLIENT_CERT"))
                .thenReturn(Optional.of(clientCertificate));

        final var serverCertificateHolder = startServerAndExtractCertificate();
        final var serverCertificate = pemEncodeCertificate(serverCertificateHolder);

        try (final var clientCertificateInputStream = IOUtils.toInputStream(clientCertificate, StandardCharsets.UTF_8);
                final var privateKeyInputStream = IOUtils.toInputStream(privateKey, StandardCharsets.UTF_8);
                final var serverCertificateInputStream = IOUtils.toInputStream(serverCertificate, StandardCharsets.UTF_8)) {
            final var client = ProviderGrpc.newBlockingStub(Grpc.newChannelBuilderForAddress("localhost", port, TlsChannelCredentials.newBuilder()
                            .keyManager(clientCertificateInputStream, privateKeyInputStream)
                            .trustManager(serverCertificateInputStream)
                            .build())
                    .build());
            final var response = client.getProviderSchema(Request.newBuilder()
                    .build());
            assertTrue(response.hasProvider());
        }

        providerServer.stop();
        providerServer.blockUntilShutdown();
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

    private String pemEncodePrivateKey(final PrivateKeyInfo privateKeyInfo) throws IOException {
        try (final var stringWriter = new StringWriter();
                final var pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PKCS8Generator(privateKeyInfo, null));
            pemWriter.flush();
            stringWriter.flush();

            return stringWriter.toString();
        }
    }

    private X509CertificateHolder startServerAndExtractCertificate() throws IOException, GeneralSecurityException {
        final var currentPrintStream = System.out;
        try (final var outputStream = new ByteArrayOutputStream();
                final var printStream = new PrintStream(outputStream)) {
            System.setOut(printStream);
            providerServer.start();
            System.setOut(currentPrintStream);
            final var protocolNegotiationLine = outputStream.toString(StandardCharsets.UTF_8)
                    .trim();

            return new X509CertificateHolder(Base64.getDecoder()
                    .decode(protocolNegotiationLine.substring(protocolNegotiationLine.lastIndexOf("|") + 1)));
        }
    }
}
