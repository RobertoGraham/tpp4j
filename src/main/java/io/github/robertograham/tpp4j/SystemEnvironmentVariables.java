package io.github.robertograham.tpp4j;

import java.util.Optional;

enum SystemEnvironmentVariables implements EnvironmentVariables {

    INSTANCE;

    @Override
    public Optional<String> get(final String name) {
        return Optional.ofNullable(System.getenv(name));
    }
}
