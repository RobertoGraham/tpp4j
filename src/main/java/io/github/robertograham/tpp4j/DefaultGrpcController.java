package io.github.robertograham.tpp4j;

import com.hashicorp.goplugin.Empty;
import com.hashicorp.goplugin.GRPCControllerGrpc.GRPCControllerImplBase;
import io.grpc.stub.StreamObserver;

final class DefaultGrpcController extends GRPCControllerImplBase {

    final ProviderServer providerServer;

    DefaultGrpcController(final ProviderServer providerServer) {
        this.providerServer = providerServer;
    }

    @Override
    public void shutdown(final Empty request, final StreamObserver<Empty> responseObserver) {
        System.err.println("[INFO] Shutting down");

        providerServer.stop();

        responseObserver.onNext(Empty.newBuilder()
                .build());
        responseObserver.onCompleted();
    }
}
