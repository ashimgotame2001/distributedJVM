package com.microjvm.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class NodeExecutorRoundTripTest {
  private static final int PORT = 19191;
  private static Path artifactRoot;

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) throws Exception {
    artifactRoot = tempDir.resolve("artifacts");
    TestArtifacts.installSamplesTasksJar(artifactRoot);
    registry.add("grpc.server.port", () -> PORT);
    registry.add("microjvm.node.id", () -> "test-node");
    registry.add("microjvm.node.artifact-root", () -> artifactRoot.toString());
    registry.add("microjvm.spike.enabled", () -> "false");
  }

  @BeforeEach
  void ensureArtifact() throws Exception {
    TestArtifacts.installSamplesTasksJar(artifactRoot);
  }

  @Test
  void executeEchoReturnsValue() throws Exception {
    JavaSerializationCodec codec = new JavaSerializationCodec();
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("127.0.0.1", PORT).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorBlockingStub stub =
          NodeExecutorGrpc.newBlockingStub(channel);
      ExecuteRequest request =
          ExecuteRequest.newBuilder()
              .setTaskId(TaskId.newBuilder().setValue("t-echo").build())
              .setEntry(
                  com.microjvm.proto.EntryPoint.newBuilder()
                      .setClassName("com.microjvm.tasks.Echo")
                      .setMethodName("echo")
                      .build())
              .setArgs(
                  com.google.protobuf.ByteString.copyFrom(
                      codec.serialize(new Object[] {"hello-remote"})))
              .addArtifactRefs(
                  com.microjvm.proto.ArtifactRef.newBuilder()
                      .setGroup("com.microjvm")
                      .setName("samples-tasks")
                      .setVersion("0.1.0")
                      .build())
              .build();

      Iterator<TaskEvent> events = stub.executeTask(request);
      String result = null;
      while (events.hasNext()) {
        TaskEvent event = events.next();
        if (event.hasResult()) {
          result = codec.deserialize(event.getResult().getPayload().toByteArray(), String.class);
        }
        if (event.hasError()) {
          throw new AssertionError(event.getError().getMessage());
        }
      }
      assertEquals("hello-remote", result);
    } finally {
      channel.shutdownNow();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void throwingTaskYieldsErrorEventAndNodeStaysUp() throws Exception {
    JavaSerializationCodec codec = new JavaSerializationCodec();
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("127.0.0.1", PORT).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorBlockingStub stub =
          NodeExecutorGrpc.newBlockingStub(channel);
      ExecuteRequest request =
          ExecuteRequest.newBuilder()
              .setTaskId(TaskId.newBuilder().setValue("t-fail").build())
              .setEntry(
                  com.microjvm.proto.EntryPoint.newBuilder()
                      .setClassName("com.microjvm.tasks.Echo")
                      .setMethodName("fail")
                      .build())
              .setArgs(
                  com.google.protobuf.ByteString.copyFrom(codec.serialize(new Object[] {"boom"})))
              .addArtifactRefs(
                  com.microjvm.proto.ArtifactRef.newBuilder()
                      .setGroup("com.microjvm")
                      .setName("samples-tasks")
                      .setVersion("0.1.0")
                      .build())
              .build();

      Iterator<TaskEvent> events = stub.executeTask(request);
      boolean sawError = false;
      while (events.hasNext()) {
        TaskEvent event = events.next();
        if (event.hasError()) {
          sawError = true;
          assertTrue(event.getError().getMessage().contains("boom"));
        }
      }
      assertTrue(sawError);

      // node still serves a second call
      ExecuteRequest ok =
          request.toBuilder()
              .setTaskId(TaskId.newBuilder().setValue("t-ok").build())
              .setEntry(
                  com.microjvm.proto.EntryPoint.newBuilder()
                      .setClassName("com.microjvm.tasks.Echo")
                      .setMethodName("echo")
                      .build())
              .setArgs(
                  com.google.protobuf.ByteString.copyFrom(codec.serialize(new Object[] {"still-up"})))
              .build();
      String result = null;
      Iterator<TaskEvent> again = stub.executeTask(ok);
      while (again.hasNext()) {
        TaskEvent event = again.next();
        if (event.hasResult()) {
          result = codec.deserialize(event.getResult().getPayload().toByteArray(), String.class);
        }
      }
      assertEquals("still-up", result);
    } finally {
      channel.shutdownNow();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
