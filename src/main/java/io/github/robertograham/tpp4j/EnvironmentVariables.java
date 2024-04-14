package io.github.robertograham.tpp4j;

import java.util.Optional;

interface EnvironmentVariables {

    static EnvironmentVariables newEnvironmentVariables() {
        return SystemEnvironmentVariables.INSTANCE;
    }

    Optional<String> get(String name);
}
