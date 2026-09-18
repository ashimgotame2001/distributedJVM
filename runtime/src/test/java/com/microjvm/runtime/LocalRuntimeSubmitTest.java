package com.microjvm.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microjvm.common.ArtifactCoordinates;
import com.microjvm.common.EntryPoint;
import com.microjvm.common.TaskStatus;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroJvm;
import com.microjvm.sdk.MicroTask;
import org.junit.jupiter.api.Test;

class LocalRuntimeSubmitTest {
  @Test
  void submitPersistsQueuedTaskQueryableById() throws Exception {
    LocalRuntime runtime = new LocalRuntime();
    MicroJvm client = new MicroJvm(runtime);

    MicroTask<String> task =
        MicroTask.of(String.class)
            .coordinates("com.example", "demo", "1.0.0")
            .entry("com.example.Demo", "run")
            .args("world")
            .build();

    MicroFuture<String> future = client.submit(task);

    assertTrue(runtime.get(future.getTaskId()).isPresent());
    StoredTask stored = runtime.get(future.getTaskId()).orElseThrow();
    assertEquals(TaskStatus.QUEUED, stored.status());
    assertEquals(
        new ArtifactCoordinates("com.example", "demo", "1.0.0"),
        stored.descriptor().codeCoordinates());
    assertEquals(new EntryPoint("com.example.Demo", "run"), stored.descriptor().entry());
  }
}
