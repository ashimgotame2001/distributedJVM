package com.distributed.jvm.coordinator.service;

import com.distributed.jvm.grpc.EmptyRequest;
import com.distributed.jvm.grpc.NodeAgentServiceGrpc;
import com.distributed.jvm.grpc.NodeHealthResponse;
import io.grpc.stub.StreamObserver;

public class NodeHealthCheckService extends NodeAgentServiceGrpc.NodeAgentServiceImplBase {

    @Override
    public void getNodeHealth(EmptyRequest request, StreamObserver<NodeHealthResponse> responseObserver) {
        super.getNodeHealth(request, responseObserver);
    }
}
