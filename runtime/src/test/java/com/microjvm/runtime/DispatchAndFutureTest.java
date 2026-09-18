package com.microjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.TaskStatus;
import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.proto.TaskError;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import com.microjvm.proto.TaskResult;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroJvm;
import com.microjvm.sdk.MicroTask;
import com.microjvm.sdk.TaskExecutionException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DispatchAndFutureTest {
  @Test
  void fixedPlacementAndResultCompleteFuture() throws Exception {
    JavaSerializationCodec codec = new JavaSerializationCodec();
    byte[] payload = codec.serialize("ok-from-node");
    NodeTarget target = new NodeTarget("127.0.0.1", 9090);
    NodeDispatchClient client =
        descriptor ->
            List.of(
                    TaskEvent.newBuilder()
                        .setTaskId(TaskId.newBuilder().setValue(descriptor.taskId()).build())
                        .setResult(
                            TaskResult.newBuilder()
                                .setPayload(ByteString.copyFrom(payload))
                                .build())
                        .build())
                .iterator();

    try (LocalRuntime runtime =
        new LocalRuntime(
            new InMemoryTaskStore(), codec, new FixedPlacement(target), client)) {
      MicroJvm microJvm = new MicroJvm(runtime);
      MicroFuture<String> future =
          microJvm.submit(
              MicroTask.of(String.class)
                  .coordinates("com.microjvm", "samples-tasks", "0.1.0")
                  .entry("com.microjvm.tasks.Echo", "echo")
                  .args("ignored")
                  .build());

      assertEquals("ok-from-node", future.get(5, TimeUnit.SECONDS));
      StoredTask stored = runtime.get(future.getTaskId()).orElseThrow();
      assertEquals(TaskStatus.COMPLETED, stored.status());
      assertEquals(target, stored.placedOn());
    }
  }

  @Test
  void errorEventFailsFuture() throws Exception {
    JavaSerializationCodec codec = new JavaSerializationCodec();
    NodeDispatchClient client =
        descriptor ->
            List.of(
                    TaskEvent.newBuilder()
                        .setTaskId(TaskId.newBuilder().setValue(descriptor.taskId()).build())
                        .setError(
                            TaskError.newBuilder()
                                .setType("java.lang.IllegalStateException")
                                .setMessage("boom")
                                .build())
                        .build())
                .iterator();

    try (LocalRuntime runtime =
        new LocalRuntime(
            new InMemoryTaskStore(),
            codec,
            new FixedPlacement(new NodeTarget("127.0.0.1", 9090)),
            client)) {
      MicroFuture<String> future =
          runtime.submit(
              MicroTask.of(String.class)
                  .coordinates("com.microjvm", "samples-tasks", "0.1.0")
                  .entry("com.microjvm.tasks.Echo", "fail")
                  .args("boom")
                  .build());

      ExecutionException ex =
          assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
      assertInstanceOf(TaskExecutionException.class, ex.getCause());
      assertTrue(ex.getCause().getMessage().contains("boom"));
      assertEquals(TaskStatus.FAILED, runtime.get(future.getTaskId()).orElseThrow().status());
    }
  }

  @Test
  void grpcRequestMapsDescriptorFields() throws Exception {
    TaskDescriptor descriptor =
        MicroTask.of(String.class)
            .coordinates("com.microjvm", "samples-tasks", "0.1.0")
            .entry("com.microjvm.tasks.Echo", "echo")
            .args("hi")
            .build()
            .toDescriptor(new JavaSerializationCodec());

    var request = GrpcNodeDispatchClient.toRequest(descriptor);
    assertEquals(descriptor.taskId(), request.getTaskId().getValue());
    assertEquals("com.microjvm.tasks.Echo", request.getEntry().getClassName());
    assertEquals("echo", request.getEntry().getMethodName());
    assertEquals("com.microjvm", request.getArtifactRefs(0).getGroup());
    assertEquals("samples-tasks", request.getArtifactRefs(0).getName());
    assertEquals("0.1.0", request.getArtifactRefs(0).getVersion());
    assertTrue(request.getArgs().size() > 0);
  }
}
