package io.github.robertograham.tpp4j;

import io.grpc.ServerBuilder;
import java.io.IOException;

public final class Server {

    public static void main(final String[] args) throws IOException, InterruptedException {
        ServerBuilder.forPort(8080)
                .addService(new DefaultProvider())
                .build()
                .start()
                .awaitTermination();
    }
}
