package io.github.robertograham.tpp4j;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

final class EnvironmentVariablesTest {

    @Test
    void newEnvironmentVariables() {
        assertSame(SystemEnvironmentVariables.INSTANCE, EnvironmentVariables.newEnvironmentVariables());
    }
}
