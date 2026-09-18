package com.microjvm.node;

import com.microjvm.proto.ExecuteRequest;
import com.microjvm.proto.NodeExecutorGrpc;
import com.microjvm.proto.TaskEvent;
import com.microjvm.proto.TaskId;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
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

  static String invokePeer(String host, int port) throws InterruptedException {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
    try {
      NodeExecutorGrpc.NodeExecutorStub stub = NodeExecutorGrpc.newStub(channel);
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<String> payload = new AtomicReference<>();
      AtomicReference<Throwable> error = new AtomicReference<>();

      ExecuteRequest request =
          ExecuteRequest.newBuilder()
              .setTaskId(TaskId.newBuilder().setValue("spike-" + System.nanoTime()).build())
              .build();

      stub.executeTask(
          request,
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

      if (!done.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for ExecuteTask response");
      }
      if (error.get() != null) {
        throw new IllegalStateException("ExecuteTask failed", error.get());
      }
      if (payload.get() == null) {
        throw new IllegalStateException("No result event received");
      }
      return payload.get();
    } finally {
      channel.shutdownNow();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
