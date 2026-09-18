package com.microjvm.node;

import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "microjvm.spike", name = "enabled", havingValue = "true")
public class RoundTripSpikeRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(RoundTripSpikeRunner.class);

  private final NodeProperties properties;
  private final ConfigurableApplicationContext context;

  public RoundTripSpikeRunner(NodeProperties properties, ConfigurableApplicationContext context) {
    this.properties = properties;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.hasPeer()) {
      throw new IllegalStateException("Spike enabled but peer host/port not configured");
    }
    try {
      String result = invokePeer(properties.getPeerHost(), properties.getPeerPort());
      log.info("Spike round-trip OK: peer returned '{}'", result);
      if (!"pong".equals(result)) {
        throw new IllegalStateException("Unexpected spike result: " + result);
      }
      System.exit(SpringApplication.exit(context, () -> 0));
    } catch (Exception e) {
      log.error("Spike round-trip failed", e);
      System.exit(SpringApplication.exit(context, () -> 1));
    }
  }

  static String invokePeer(String host, int port) throws Exception {
    JavaSerializationCodec codec = new JavaSerializationCodec();
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorStub stub = NodeExecutorGrpc.newStub(channel);
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<byte[]> payload = new AtomicReference<>();
      AtomicReference<Throwable> error = new AtomicReference<>();

      ExecuteRequest request =
          ExecuteRequest.newBuilder()
              .setTaskId(TaskId.newBuilder().setValue("spike-" + System.nanoTime()).build())
              .setEntry(
                  com.microjvm.proto.EntryPoint.newBuilder()
                      .setClassName("com.microjvm.tasks.Echo")
                      .setMethodName("echo")
                      .build())
              .setArgs(com.google.protobuf.ByteString.copyFrom(codec.serialize(new Object[] {"pong"})))
              .addArtifactRefs(
                  com.microjvm.proto.ArtifactRef.newBuilder()
                      .setGroup("com.microjvm")
                      .setName("samples-tasks")
                      .setVersion("0.1.0")
                      .build())
              .build();

      stub.executeTask(
          request,
          new StreamObserver<>() {
            @Override
            public void onNext(TaskEvent value) {
              if (value.hasResult()) {
                payload.set(value.getResult().getPayload().toByteArray());
              }
              if (value.hasError()) {
                error.set(
                    new IllegalStateException(
                        value.getError().getType() + ": " + value.getError().getMessage()));
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

      if (!done.await(30, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for ExecuteTask response");
      }
      if (error.get() != null) {
        throw new IllegalStateException("ExecuteTask failed", error.get());
      }
      if (payload.get() == null) {
        throw new IllegalStateException("No result event received");
      }
      return codec.deserialize(payload.get(), String.class);
    } finally {
      channel.shutdownNow();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
