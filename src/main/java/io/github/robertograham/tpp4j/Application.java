package io.github.robertograham.tpp4j;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class Application {

    public static void main(final String[] args) throws IOException, InterruptedException, GeneralSecurityException {
        new ProviderServer(new SystemEnvironmentVariables())
                .awaitTermination();
    }
}
