package io.github.robertograham.tpp4j;

import com.hashicorp.goplugin.Empty;
import com.hashicorp.goplugin.GRPCControllerGrpc.GRPCControllerImplBase;
import io.grpc.stub.StreamObserver;

final class DefaultGrpcController extends GRPCControllerImplBase {

    private final Runnable serverShutdown;

    DefaultGrpcController(final Runnable serverShutdown) {
        this.serverShutdown = serverShutdown;
    }

    @Override
    public void shutdown(final Empty request, final StreamObserver<Empty> responseObserver) {
        System.err.println("[INFO] Shutting down");

        serverShutdown.run();

        responseObserver.onNext(Empty.newBuilder()
                .build());
        responseObserver.onCompleted();
    }
}
