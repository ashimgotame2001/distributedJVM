package com.microjvm.runtime;

/** Always places every task on the same configured node (pre-scoring placeholder). */
public final class FixedPlacement {
  private final NodeTarget target;

  public FixedPlacement(NodeTarget target) {
    this.target = java.util.Objects.requireNonNull(target, "target");
  }

  public NodeTarget place(String taskId) {
    return target;
  }

  public NodeTarget target() {
    return target;
  }
}
