package io.github.robertograham.tpp4j;

interface PluginServer {

    static PluginServer newPluginServer() {
        return new DefaultPluginServer(EnvironmentVariables::newEnvironmentVariables, PrivateCredential::newPrivateCredential);
    }

    void run();

    void awaitShutdown();
}
