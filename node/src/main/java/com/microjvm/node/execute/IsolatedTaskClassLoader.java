package com.microjvm.node.execute;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public final class IsolatedTaskClassLoader extends URLClassLoader {
  public IsolatedTaskClassLoader(List<Path> jars, ClassLoader parent) {
    super(toUrls(jars), parent);
  }

  private static URL[] toUrls(List<Path> jars) {
    return jars.stream()
        .map(
            path -> {
              try {
                return path.toUri().toURL();
              } catch (Exception e) {
                throw new IllegalArgumentException("Bad jar path: " + path, e);
              }
            })
        .toArray(URL[]::new);
  }
}
