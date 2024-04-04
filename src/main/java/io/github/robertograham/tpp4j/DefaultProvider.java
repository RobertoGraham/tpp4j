package io.github.robertograham.tpp4j;

import io.grpc.stub.StreamObserver;
import io.terraform.tfplugin6.GetProviderSchema.Request;
import io.terraform.tfplugin6.GetProviderSchema.Response;
import io.terraform.tfplugin6.ProviderGrpc.ProviderImplBase;
import io.terraform.tfplugin6.Schema;
import io.terraform.tfplugin6.Schema.Block;
import io.terraform.tfplugin6.StringKind;

final class DefaultProvider extends ProviderImplBase {

    private static final long PROVIDER_VERSION = 1L;

    @Override
    public void getProviderSchema(final Request request, final StreamObserver<Response> responseObserver) {
        System.err.println("[INFO] Getting provider schema");

        responseObserver.onNext(Response.newBuilder()
                .setProvider(Schema.newBuilder()
                        .setVersion(PROVIDER_VERSION)
                        .setBlock(Block.newBuilder()
                                .setVersion(PROVIDER_VERSION)
                                .setDescription("Test provider block")
                                .setDescriptionKind(StringKind.PLAIN)
                                .setDeprecated(false)
                                .build())
                        .build())
                .build());
        responseObserver.onCompleted();
    }
}
