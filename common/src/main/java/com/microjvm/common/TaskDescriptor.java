package com.microjvm.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TaskDescriptor implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String taskId;
  private final ArtifactCoordinates codeCoordinates;
  private final EntryPoint entry;
  private final byte[] args;
  private final TaskConstraints constraints;
  private final List<String> dependencies;
  private final ResultSpec resultSpec;

  private TaskDescriptor(Builder builder) {
    this.taskId = builder.taskId != null ? builder.taskId : UUID.randomUUID().toString();
    this.codeCoordinates = Objects.requireNonNull(builder.codeCoordinates, "codeCoordinates");
    this.entry = Objects.requireNonNull(builder.entry, "entry");
    this.args = builder.args != null ? builder.args.clone() : new byte[0];
    this.constraints =
        builder.constraints != null ? builder.constraints : TaskConstraints.defaults();
    this.dependencies = List.copyOf(builder.dependencies);
    this.resultSpec = builder.resultSpec != null ? builder.resultSpec : ResultSpec.BY_VALUE;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String taskId() {
    return taskId;
  }

  public ArtifactCoordinates codeCoordinates() {
    return codeCoordinates;
  }

  public EntryPoint entry() {
    return entry;
  }

  public byte[] args() {
    return args.clone();
  }

  public TaskConstraints constraints() {
    return constraints;
  }

  public List<String> dependencies() {
    return dependencies;
  }

  public ResultSpec resultSpec() {
    return resultSpec;
  }

  public static final class Builder {
    private String taskId;
    private ArtifactCoordinates codeCoordinates;
    private EntryPoint entry;
    private byte[] args;
    private TaskConstraints constraints;
    private final List<String> dependencies = new ArrayList<>();
    private ResultSpec resultSpec;

    public Builder taskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder codeCoordinates(ArtifactCoordinates codeCoordinates) {
      this.codeCoordinates = codeCoordinates;
      return this;
    }

    public Builder entry(EntryPoint entry) {
      this.entry = entry;
      return this;
    }

    public Builder args(byte[] args) {
      this.args = args;
      return this;
    }

    public Builder constraints(TaskConstraints constraints) {
      this.constraints = constraints;
      return this;
    }

    public Builder dependencies(List<String> dependencies) {
      this.dependencies.clear();
      if (dependencies != null) {
        this.dependencies.addAll(dependencies);
      }
      return this;
    }

    public Builder addDependency(String taskId) {
      this.dependencies.add(taskId);
      return this;
    }

    public Builder resultSpec(ResultSpec resultSpec) {
      this.resultSpec = resultSpec;
      return this;
    }

    public TaskDescriptor build() {
      return new TaskDescriptor(this);
    }
  }
}
