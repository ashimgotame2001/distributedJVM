package com.microjvm.runtime;

import com.microjvm.common.ArtifactCoordinates;
import com.microjvm.common.TaskDescriptor;
import com.microjvm.proto.ArtifactRef;
import com.microjvm.proto.EntryPoint;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class GrpcNodeDispatchClient implements NodeDispatchClient {
  private final ManagedChannel channel;
  private final NodeExecutorGrpc.NodeExecutorBlockingStub stub;

  public GrpcNodeDispatchClient(NodeTarget target) {
    this(
        ManagedChannelBuilder.forAddress(target.host(), target.port())
            .usePlaintext()
            .build());
  }

  GrpcNodeDispatchClient(ManagedChannel channel) {
    this.channel = Objects.requireNonNull(channel, "channel");
    this.stub = NodeExecutorGrpc.newBlockingStub(channel);
  }

  @Override
  public Iterator<TaskEvent> execute(TaskDescriptor descriptor) {
    return stub.executeTask(toRequest(descriptor));
  }

  static ExecuteRequest toRequest(TaskDescriptor descriptor) {
    ArtifactCoordinates coords = descriptor.codeCoordinates();
    return ExecuteRequest.newBuilder()
        .setTaskId(TaskId.newBuilder().setValue(descriptor.taskId()).build())
        .setEntry(
            EntryPoint.newBuilder()
                .setClassName(descriptor.entry().className())
                .setMethodName(descriptor.entry().methodName())
                .build())
        .setArgs(com.google.protobuf.ByteString.copyFrom(descriptor.args()))
        .addArtifactRefs(
            ArtifactRef.newBuilder()
                .setGroup(coords.group())
                .setName(coords.name())
                .setVersion(coords.version())
                .build())
        .build();
  }

  @Override
  public void close() {
    channel.shutdown();
    try {
      channel.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      channel.shutdownNow();
    }
  }
}
