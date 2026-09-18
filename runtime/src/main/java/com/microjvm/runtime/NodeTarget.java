package com.microjvm.runtime;

import java.util.Objects;

public final class NodeTarget {
  private final String host;
  private final int port;

  public NodeTarget(String host, int port) {
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("host required");
    }
    if (port <= 0) {
      throw new IllegalArgumentException("port must be positive");
    }
    this.host = host;
    this.port = port;
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public String address() {
    return host + ":" + port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NodeTarget that)) {
      return false;
    }
    return port == that.port && host.equals(that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, port);
  }

  @Override
  public String toString() {
    return address();
  }
}
