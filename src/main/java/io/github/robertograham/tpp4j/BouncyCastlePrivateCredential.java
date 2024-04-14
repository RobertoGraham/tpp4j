package io.github.robertograham.tpp4j;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.bc.BcX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECNamedDomainParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.openssl.MiscPEMGenerator;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcECContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemWriter;

final class BouncyCastlePrivateCredential implements PrivateCredential {

    private final X509CertificateHolder x509CertificateHolder;
    private final AsymmetricKeyParameter privateKey;

    BouncyCastlePrivateCredential() {
        final var keyPair = createKeyPair();
        x509CertificateHolder = createCertificate(keyPair);
        privateKey = keyPair.getPrivate();
    }

    private AsymmetricCipherKeyPair createKeyPair() {
        final var generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(
                new ECNamedDomainParameters(SECObjectIdentifiers.secp384r1, SECNamedCurves.getByOID(SECObjectIdentifiers.secp384r1)), null));

        return generator.generateKeyPair();
    }

    private X509CertificateHolder createCertificate(final AsymmetricCipherKeyPair keyPair) {
        final var issuerAndSubject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "localhost")
                .build();
        final var serial = BigInteger.valueOf(Instant.now()
                .toEpochMilli());
        final var now = Instant.now();
        final var signatureAlgorithmIdentifier = new AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA384);
        final var digestAlgorithmIdentifier = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha384);

        try {
            final var bcContentSignerBuilder = new BcECContentSignerBuilder(signatureAlgorithmIdentifier, digestAlgorithmIdentifier)
                    .build(keyPair.getPrivate());

            return new BcX509v3CertificateBuilder(issuerAndSubject, serial, Date.from(now), Date.from(now.plusSeconds(TimeUnit.DAYS.toSeconds(1L))),
                    issuerAndSubject, keyPair.getPublic())
                    .addExtension(Extension.subjectAlternativeName, false, new DERSequence((new GeneralName(GeneralName.dNSName, "localhost"))))
                    .build(bcContentSignerBuilder);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to create X.509 certificate builder.", exception);
        } catch (final OperatorCreationException exception) {
            throw new UnsupportedOperationException("Failed to create EC content signer.", exception);
        }
    }

    @Override
    public String rfc7468EncodedX509Certificate() {
        try (final var stringWriter = new StringWriter();
                final var pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new MiscPEMGenerator(x509CertificateHolder));
            pemWriter.flush();

            return stringWriter.toString();
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to RFC 7468 encode X.509 certificate.", exception);
        }
    }

    @Override
    public String rfc7468EncodedPkcs8PrivateKeyInfo() {
        try (final var stringWriter = new StringWriter();
                final var pemWriter = new PemWriter(stringWriter)) {
            pemWriter.writeObject(new PKCS8Generator(PrivateKeyInfoFactory.createPrivateKeyInfo(privateKey), null));
            pemWriter.flush();

            return stringWriter.toString();
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to RFC 7468 encode PKCS #8 private-key information.", exception);
        }
    }

    @Override
    public String base64EncodedX509CertificateWithoutPadding() {
        try {
            return Base64.getEncoder()
                    .withoutPadding()
                    .encodeToString(x509CertificateHolder.getEncoded());
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to Base64 encode X.509 certificate.", exception);
        }
    }
}
