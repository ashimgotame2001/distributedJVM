package com.microjvm.common;

import java.io.Serializable;

public final class TaskConstraints implements Serializable {
  private static final long serialVersionUID = 1L;
  private final int minHeapMb;
  private final boolean requiresGpu;
  private final int priority;

  public static TaskConstraints defaults() {
    return new TaskConstraints(0, false, 0);
  }

  public TaskConstraints(int minHeapMb, boolean requiresGpu, int priority) {
    this.minHeapMb = minHeapMb;
    this.requiresGpu = requiresGpu;
    this.priority = priority;
  }

  public int minHeapMb() {
    return minHeapMb;
  }

  public boolean requiresGpu() {
    return requiresGpu;
  }

  public int priority() {
    return priority;
  }
}
