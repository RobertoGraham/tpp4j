package io.github.robertograham.tpp4j;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;

record PrivateCredential(X509CertificateHolder certificate, PrivateKeyInfo privateKey) {

}
