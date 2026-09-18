package com.microjvm.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microjvm.proto.Ack;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.HealthRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.NodeSnapshot;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class NoOpRoundTripTest {
  private static final int PORT = 19090;

  @DynamicPropertySource
  static void grpcPort(DynamicPropertyRegistry registry) {
    registry.add("grpc.server.port", () -> PORT);
    registry.add("microjvm.node.id", () -> "test-node");
    registry.add("microjvm.spike.enabled", () -> "false");
  }

  @Test
  void executeTaskReturnsConstantPong() throws Exception {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("127.0.0.1", PORT).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorStub stub = NodeExecutorGrpc.newStub(channel);
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<String> payload = new AtomicReference<>();
      AtomicReference<Throwable> error = new AtomicReference<>();

      stub.executeTask(
          ExecuteRequest.newBuilder()
              .setTaskId(TaskId.newBuilder().setValue("t-1").build())
              .build(),
          new StreamObserver<>() {
            @Override
            public void onNext(TaskEvent value) {
              if (value.hasResult()) {
                payload.set(value.getResult().getPayload().toString(StandardCharsets.UTF_8));
              }
            }

            @Override
            public void onError(Throwable t) {
              error.set(t);
              done.countDown();
            }

            @Override
            public void onCompleted() {
              done.countDown();
            }
          });

      done.await(10, TimeUnit.SECONDS);
      if (error.get() != null) {
        throw new AssertionError(error.get());
      }
      assertEquals("pong", payload.get());
    } finally {
      channel.shutdownNow();
    }
  }

  @Test
  void cancelAndHealthRespond() throws Exception {
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("127.0.0.1", PORT).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorBlockingStub stub =
          NodeExecutorGrpc.newBlockingStub(channel);
      Ack ack = stub.cancelTask(TaskId.newBuilder().setValue("t-2").build());
      assertEquals(true, ack.getOk());

      NodeSnapshot snapshot = stub.reportHealth(HealthRequest.getDefaultInstance()).next();
      assertEquals("test-node", snapshot.getNodeId());
    } finally {
      channel.shutdownNow();
    }
  }
}
