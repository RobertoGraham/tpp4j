package io.github.robertograham.tpp4j;

public final class Application {

    public static void main(final String[] args) {
        final var pluginServer = PluginServer.newPluginServer();
        pluginServer.run();
        pluginServer.awaitShutdown();
    }
}
