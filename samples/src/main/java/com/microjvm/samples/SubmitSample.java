package com.microjvm.samples;

import com.microjvm.common.TaskStatus;
import com.microjvm.runtime.LocalRuntime;
import com.microjvm.runtime.StoredTask;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroJvm;
import com.microjvm.sdk.MicroTask;

public final class SubmitSample {
  public static void main(String[] args) throws Exception {
    LocalRuntime runtime = new LocalRuntime();
    MicroJvm microJvm = new MicroJvm(runtime);

    MicroTask<String> task =
        MicroTask.of(String.class)
            .coordinates("com.microjvm", "samples", "0.1.0")
            .entry("com.microjvm.samples.Echo", "echo")
            .args("hello-microjvm")
            .build();

    MicroFuture<String> future = microJvm.submit(task);
    StoredTask stored =
        runtime
            .get(future.getTaskId())
            .orElseThrow(() -> new IllegalStateException("task missing from store"));

    System.out.println("submitted taskId=" + future.getTaskId());
    System.out.println("status=" + stored.status());
    if (stored.status() != TaskStatus.QUEUED) {
      throw new IllegalStateException("expected QUEUED, got " + stored.status());
    }
  }
}
