package com.microjvm.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microjvm.common.TaskStatus;
import com.microjvm.runtime.FixedPlacement;
import com.microjvm.runtime.GrpcNodeDispatchClient;
import com.microjvm.runtime.InMemoryTaskStore;
import com.microjvm.runtime.LocalRuntime;
import com.microjvm.runtime.NodeTarget;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroJvm;
import com.microjvm.sdk.MicroTask;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class EndToEndDispatchTest {
  private static final int PORT = 19192;
  private static Path artifactRoot;

  @TempDir static Path tempDir;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) throws Exception {
    artifactRoot = tempDir.resolve("artifacts");
    TestArtifacts.installSamplesTasksJar(artifactRoot);
    registry.add("grpc.server.port", () -> PORT);
    registry.add("microjvm.node.id", () -> "e2e-node");
    registry.add("microjvm.node.artifact-root", () -> artifactRoot.toString());
    registry.add("microjvm.spike.enabled", () -> "false");
  }

  @Test
  void submitOnRuntimeRunsOnNodeAndResolvesFuture() throws Exception {
    TestArtifacts.installSamplesTasksJar(artifactRoot);
    NodeTarget target = new NodeTarget("127.0.0.1", PORT);
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
                  .args("from-a-to-b")
                  .build());

      assertEquals("from-a-to-b", future.get(20, TimeUnit.SECONDS));
      assertEquals(TaskStatus.COMPLETED, runtime.get(future.getTaskId()).orElseThrow().status());
      assertEquals(target, runtime.get(future.getTaskId()).orElseThrow().placedOn());
    }
  }
}
