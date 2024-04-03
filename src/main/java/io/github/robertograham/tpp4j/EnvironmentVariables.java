package io.github.robertograham.tpp4j;

import java.util.Optional;

interface EnvironmentVariables {

    Optional<String> get(String name);
}
