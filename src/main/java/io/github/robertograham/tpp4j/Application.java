package io.github.robertograham.tpp4j;

import java.io.IOException;

public final class Application {

    public static void main(final String[] args) throws IOException, InterruptedException {
        new ProviderServer(new SystemEnvironmentVariables())
                .awaitTermination();
    }
}
