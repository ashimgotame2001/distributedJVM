package com.microjvm.sdk;

import com.microjvm.common.ArtifactCoordinates;
import com.microjvm.common.EntryPoint;
import com.microjvm.common.ResultSpec;
import com.microjvm.common.TaskConstraints;
import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.codec.TaskCodec;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MicroTask<T> {
  private final ArtifactCoordinates coordinates;
  private final EntryPoint entry;
  private final Object[] args;
  private final TaskConstraints constraints;
  private final List<String> dependencies;
  private final ResultSpec resultSpec;
  private final Class<T> resultType;

  private MicroTask(Builder<T> builder) {
    this.coordinates = Objects.requireNonNull(builder.coordinates, "coordinates");
    this.entry = Objects.requireNonNull(builder.entry, "entry");
    this.args = builder.args != null ? builder.args.clone() : new Object[0];
    this.constraints =
        builder.constraints != null ? builder.constraints : TaskConstraints.defaults();
    this.dependencies = List.copyOf(builder.dependencies);
    this.resultSpec = builder.resultSpec != null ? builder.resultSpec : ResultSpec.BY_VALUE;
    this.resultType = Objects.requireNonNull(builder.resultType, "resultType");
  }

  public static <T> Builder<T> of(Class<T> resultType) {
    return new Builder<>(resultType);
  }

  public ArtifactCoordinates coordinates() {
    return coordinates;
  }

  public EntryPoint entry() {
    return entry;
  }

  public Object[] args() {
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

  public Class<T> resultType() {
    return resultType;
  }

  public TaskDescriptor toDescriptor(TaskCodec codec) throws IOException {
    byte[] encoded = codec.serialize(args);
    return TaskDescriptor.builder()
        .codeCoordinates(coordinates)
        .entry(entry)
        .args(encoded)
        .constraints(constraints)
        .dependencies(dependencies)
        .resultSpec(resultSpec)
        .build();
  }

  public static final class Builder<T> {
    private final Class<T> resultType;
    private ArtifactCoordinates coordinates;
    private EntryPoint entry;
    private Object[] args;
    private TaskConstraints constraints;
    private final List<String> dependencies = new ArrayList<>();
    private ResultSpec resultSpec;

    private Builder(Class<T> resultType) {
      this.resultType = resultType;
    }

    public Builder<T> coordinates(String group, String name, String version) {
      this.coordinates = new ArtifactCoordinates(group, name, version);
      return this;
    }

    public Builder<T> coordinates(ArtifactCoordinates coordinates) {
      this.coordinates = coordinates;
      return this;
    }

    public Builder<T> entry(String className, String methodName) {
      this.entry = new EntryPoint(className, methodName);
      return this;
    }

    public Builder<T> entry(EntryPoint entry) {
      this.entry = entry;
      return this;
    }

    public Builder<T> args(Object... args) {
      for (Object arg : args) {
        if (arg != null && !(arg instanceof Serializable)) {
          throw new IllegalArgumentException(
              "Task args must be Serializable, got " + arg.getClass().getName());
        }
      }
      this.args = args;
      return this;
    }

    public Builder<T> constraints(TaskConstraints constraints) {
      this.constraints = constraints;
      return this;
    }

    public Builder<T> dependsOn(String taskId) {
      this.dependencies.add(taskId);
      return this;
    }

    public Builder<T> resultSpec(ResultSpec resultSpec) {
      this.resultSpec = resultSpec;
      return this;
    }

    public MicroTask<T> build() {
      return new MicroTask<>(this);
    }
  }
}
