package com.microjvm.samples;

import com.microjvm.common.TaskStatus;
import com.microjvm.runtime.FixedPlacement;
import com.microjvm.runtime.GrpcNodeDispatchClient;
import com.microjvm.runtime.InMemoryTaskStore;
import com.microjvm.runtime.LocalRuntime;
import com.microjvm.runtime.NodeTarget;
import com.microjvm.runtime.StoredTask;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroJvm;
import com.microjvm.sdk.MicroTask;
import java.util.concurrent.TimeUnit;

public final class SubmitSample {
  public static void main(String[] args) throws Exception {
    String host = env("MICROJVM_TARGET_HOST", "");
    String portText = env("MICROJVM_TARGET_PORT", "");

    if (host.isBlank() || portText.isBlank()) {
      runStoreOnlyDemo();
      return;
    }

    NodeTarget target = new NodeTarget(host, Integer.parseInt(portText));
    try (GrpcNodeDispatchClient client = new GrpcNodeDispatchClient(target);
        LocalRuntime runtime =
            new LocalRuntime(
                new InMemoryTaskStore(),
                new com.microjvm.common.codec.JavaSerializationCodec(),
                new FixedPlacement(target),
                client)) {
      MicroJvm microJvm = new MicroJvm(runtime);
      MicroFuture<String> future =
          microJvm.submit(
              MicroTask.of(String.class)
                  .coordinates("com.microjvm", "samples-tasks", "0.1.0")
                  .entry("com.microjvm.tasks.Echo", "echo")
                  .args("hello-microjvm")
                  .build());

      String value = future.get(30, TimeUnit.SECONDS);
      StoredTask stored = runtime.get(future.getTaskId()).orElseThrow();
      System.out.println("taskId=" + future.getTaskId());
      System.out.println("status=" + stored.status());
      System.out.println("placedOn=" + stored.placedOn());
      System.out.println("result=" + value);
      if (stored.status() != TaskStatus.COMPLETED) {
        throw new IllegalStateException("expected COMPLETED");
      }
    }
  }

  private static void runStoreOnlyDemo() throws Exception {
    LocalRuntime runtime = new LocalRuntime();
    MicroJvm microJvm = new MicroJvm(runtime);
    MicroFuture<String> future =
        microJvm.submit(
            MicroTask.of(String.class)
                .coordinates("com.microjvm", "samples-tasks", "0.1.0")
                .entry("com.microjvm.tasks.Echo", "echo")
                .args("hello-microjvm")
                .build());
    StoredTask stored = runtime.get(future.getTaskId()).orElseThrow();
    System.out.println("submitted taskId=" + future.getTaskId());
    System.out.println("status=" + stored.status());
    System.out.println("(set MICROJVM_TARGET_HOST/PORT to dispatch to a live node)");
  }

  private static String env(String key, String defaultValue) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
