package com.microjvm.common;

import java.io.Serializable;
import java.util.Objects;

public final class ArtifactCoordinates implements Serializable {
  private static final long serialVersionUID = 1L;
  private final String group;
  private final String name;
  private final String version;

  public ArtifactCoordinates(String group, String name, String version) {
    this.group = Objects.requireNonNull(group, "group");
    this.name = Objects.requireNonNull(name, "name");
    this.version = Objects.requireNonNull(version, "version");
  }

  public String group() {
    return group;
  }

  public String name() {
    return name;
  }

  public String version() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArtifactCoordinates that)) {
      return false;
    }
    return group.equals(that.group) && name.equals(that.name) && version.equals(that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, name, version);
  }

  @Override
  public String toString() {
    return group + ":" + name + ":" + version;
  }
}
