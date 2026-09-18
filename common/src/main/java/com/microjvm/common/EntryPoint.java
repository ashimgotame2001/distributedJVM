package com.microjvm.common;

import java.io.Serializable;
import java.util.Objects;

public final class EntryPoint implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String className;
  private final String methodName;

  public EntryPoint(String className, String methodName) {
    this.className = Objects.requireNonNull(className, "className");
    this.methodName = Objects.requireNonNull(methodName, "methodName");
  }

  public String className() {
    return className;
  }

  public String methodName() {
    return methodName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntryPoint that)) {
      return false;
    }
    return className.equals(that.className) && methodName.equals(that.methodName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, methodName);
  }
}
