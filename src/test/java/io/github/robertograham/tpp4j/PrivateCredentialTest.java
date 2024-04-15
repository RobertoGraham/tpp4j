package io.github.robertograham.tpp4j;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

final class PrivateCredentialTest {

    @Test
    void newPrivateCredential() {
        assertInstanceOf(BouncyCastlePrivateCredential.class, PrivateCredential.newPrivateCredential());
    }
}
