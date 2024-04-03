package io.github.robertograham.tpp4j;

import java.util.Optional;

final class SystemEnvironmentVariables implements EnvironmentVariables {

    @Override
    public Optional<String> get(final String name) {
        return Optional.ofNullable(System.getenv(name));
    }
}
