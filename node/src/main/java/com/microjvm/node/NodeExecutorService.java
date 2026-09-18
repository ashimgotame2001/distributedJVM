package com.microjvm.node;

import com.google.protobuf.ByteString;
import com.microjvm.common.ArtifactCoordinates;
import com.microjvm.common.codec.TaskCodec;
import com.microjvm.node.artifact.LocalArtifactResolver;
import com.microjvm.node.execute.IsolatedTaskClassLoader;
import com.microjvm.node.execute.ReflectiveTaskInvoker;
import com.microjvm.node.execute.TaskWorkerPool;
import com.microjvm.proto.Ack;
import com.microjvm.proto.ArtifactRef;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.HealthRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.NodeSnapshot;
import com.microjvm.proto.TaskError;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import com.microjvm.proto.TaskProgress;
import com.microjvm.proto.TaskResult;
import io.grpc.stub.StreamObserver;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class NodeExecutorService extends NodeExecutorGrpc.NodeExecutorImplBase {
  private final NodeProperties properties;
  private final LocalArtifactResolver artifacts;
  private final ReflectiveTaskInvoker invoker;
  private final TaskWorkerPool workers;
  private final TaskCodec codec;
  private final AtomicInteger activeTasks = new AtomicInteger();

  public NodeExecutorService(
      NodeProperties properties,
      LocalArtifactResolver artifacts,
      ReflectiveTaskInvoker invoker,
      TaskWorkerPool workers,
      TaskCodec codec) {
    this.properties = properties;
    this.artifacts = artifacts;
    this.invoker = invoker;
    this.workers = workers;
    this.codec = codec;
  }

  @Override
  public void executeTask(ExecuteRequest request, StreamObserver<TaskEvent> responseObserver) {
    workers.submit(() -> runTask(request, responseObserver));
  }

  private void runTask(ExecuteRequest request, StreamObserver<TaskEvent> responseObserver) {
    activeTasks.incrementAndGet();
    IsolatedTaskClassLoader classLoader = null;
    try {
      responseObserver.onNext(
          TaskEvent.newBuilder()
              .setTaskId(request.getTaskId())
              .setProgress(TaskProgress.newBuilder().setMessage("running").setFraction(0).build())
              .build());

      List<Path> jars = resolveJars(request);
      classLoader = new IsolatedTaskClassLoader(jars, ClassLoader.getPlatformClassLoader());

      Object value =
          invoker.invoke(
              classLoader,
              request.getEntry().getClassName(),
              request.getEntry().getMethodName(),
              request.getArgs().toByteArray());

      byte[] payload = value == null ? new byte[0] : codec.serialize(value);
      responseObserver.onNext(
          TaskEvent.newBuilder()
              .setTaskId(request.getTaskId())
              .setResult(TaskResult.newBuilder().setPayload(ByteString.copyFrom(payload)).build())
              .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      Throwable cause = unwrap(e);
      responseObserver.onNext(
          TaskEvent.newBuilder()
              .setTaskId(request.getTaskId())
              .setError(
                  TaskError.newBuilder()
                      .setType(cause.getClass().getName())
                      .setMessage(cause.getMessage() == null ? cause.toString() : cause.getMessage())
                      .build())
              .build());
      responseObserver.onCompleted();
    } finally {
      activeTasks.decrementAndGet();
      if (classLoader != null) {
        try {
          classLoader.close();
        } catch (Exception ignored) {
          // best-effort release
        }
      }
    }
  }

  private List<Path> resolveJars(ExecuteRequest request) throws Exception {
    List<Path> jars = new ArrayList<>();
    for (ArtifactRef ref : request.getArtifactRefsList()) {
      jars.addAll(
          artifacts.resolve(
              new ArtifactCoordinates(ref.getGroup(), ref.getName(), ref.getVersion())));
    }
    if (jars.isEmpty()) {
      throw new IllegalArgumentException("ExecuteRequest has no artifact refs");
    }
    return jars;
  }

  private static Throwable unwrap(Exception e) {
    if (e instanceof InvocationTargetException ite && ite.getCause() != null) {
      return ite.getCause();
    }
    return e;
  }

  @Override
  public void cancelTask(TaskId request, StreamObserver<Ack> responseObserver) {
    responseObserver.onNext(Ack.newBuilder().setOk(true).setMessage("cancel accepted").build());
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
            .setActiveTasks(activeTasks.get())
            .setTimestampEpochMs(System.currentTimeMillis())
            .build();
    responseObserver.onNext(snapshot);
    responseObserver.onCompleted();
  }
}
