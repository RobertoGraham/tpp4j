package io.github.robertograham.tpp4j;

interface PrivateCredential {

    static PrivateCredential newPrivateCredential() {
        return new BouncyCastlePrivateCredential();
    }

    String rfc7468EncodedX509Certificate();

    String rfc7468EncodedPkcs8PrivateKeyInfo();

    String base64EncodedX509CertificateWithoutPadding();
}
