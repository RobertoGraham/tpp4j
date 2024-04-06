package io.github.robertograham.tpp4j;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.util.PrimeCertaintyCalculator;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

final class PrivateCredentialHelper {

    private PrivateCredentialHelper() {
    }

    static PrivateCredential generate() throws GeneralSecurityException, IOException {
        try {
            final var keyPair = createKeyPair();
            final var certificate = createCertificate(keyPair);

            return new PrivateCredential(certificate, PrivateKeyInfoFactory.createPrivateKeyInfo(keyPair.getPrivate()));
        } catch (final OperatorCreationException | CertIOException | GeneralSecurityException exception) {
            throw new GeneralSecurityException("Failed to generate private credential.", exception);
        }
    }

    private static AsymmetricCipherKeyPair createKeyPair() throws GeneralSecurityException {
        final var generator = new RSAKeyPairGenerator();
        generator.init(new RSAKeyGenerationParameters(RSAKeyGenParameterSpec.F4, new SecureRandom(), 4096,
                PrimeCertaintyCalculator.getDefaultCertainty(4096)));

        return generator.generateKeyPair();
    }

    private static X509CertificateHolder createCertificate(final AsymmetricCipherKeyPair keyPair)
            throws IOException, OperatorCreationException {
        final var issuerAndSubject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "tpp4j")
                .build();
        final var serial = BigInteger.valueOf(Instant.now()
                .toEpochMilli());
        final var now = Instant.now();
        final var signatureAlgorithmIdentifier = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        final var digestAlgorithmIdentifier = new DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithmIdentifier);

        return new X509v3CertificateBuilder(issuerAndSubject, serial, Date.from(now),
                Date.from(now.plusSeconds(TimeUnit.DAYS.toSeconds(1L))), issuerAndSubject,
                SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.getPublic()))
                .addExtension(Extension.subjectAlternativeName, false, new DERSequence((new GeneralName(GeneralName.dNSName, "localhost"))))
                .build(new BcRSAContentSignerBuilder(signatureAlgorithmIdentifier, digestAlgorithmIdentifier)
                        .build(keyPair.getPrivate()));
    }
}
