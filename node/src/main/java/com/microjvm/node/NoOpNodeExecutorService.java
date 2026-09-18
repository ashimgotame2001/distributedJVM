package com.microjvm.node;

import com.google.protobuf.ByteString;
import com.microjvm.proto.Ack;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.HealthRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.NodeSnapshot;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import com.microjvm.proto.TaskResult;
import io.grpc.stub.StreamObserver;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class NoOpNodeExecutorService extends NodeExecutorGrpc.NodeExecutorImplBase {
  private final NodeProperties properties;

  public NoOpNodeExecutorService(NodeProperties properties) {
    this.properties = properties;
  }

  @Override
  public void executeTask(ExecuteRequest request, StreamObserver<TaskEvent> responseObserver) {
    TaskEvent event =
        TaskEvent.newBuilder()
            .setTaskId(request.getTaskId())
            .setResult(
                TaskResult.newBuilder()
                    .setPayload(ByteString.copyFrom("pong", StandardCharsets.UTF_8))
                    .build())
            .build();
    responseObserver.onNext(event);
    responseObserver.onCompleted();
  }

  @Override
  public void cancelTask(TaskId request, StreamObserver<Ack> responseObserver) {
    responseObserver.onNext(Ack.newBuilder().setOk(true).setMessage("cancelled").build());
    responseObserver.onCompleted();
  }

  @Override
  public void reportHealth(HealthRequest request, StreamObserver<NodeSnapshot> responseObserver) {
    MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    NodeSnapshot snapshot =
        NodeSnapshot.newBuilder()
            .setNodeId(properties.getId())
            .setCpuLoad(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
            .setHeapUsedBytes(memory.getHeapMemoryUsage().getUsed())
            .setHeapMaxBytes(memory.getHeapMemoryUsage().getMax())
            .setActiveTasks(0)
            .setTimestampEpochMs(System.currentTimeMillis())
            .build();
    responseObserver.onNext(snapshot);
    responseObserver.onCompleted();
  }
}
