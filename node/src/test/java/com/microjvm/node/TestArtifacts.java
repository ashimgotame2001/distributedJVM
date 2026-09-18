package com.microjvm.node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

final class TestArtifacts {
  private TestArtifacts() {}

  static Path installSamplesTasksJar(Path artifactRoot) throws Exception {
    Path source =
        Path.of(
            com.microjvm.tasks.Echo.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    Path dir = artifactRoot.resolve("com/microjvm/samples-tasks/0.1.0");
    Files.createDirectories(dir);
    Path target = dir.resolve("samples-tasks.jar");
    if (Files.isDirectory(source)) {
      jarDirectory(source, target);
    } else {
      Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return target;
  }

  static void writeVersionedJar(Path jar, String versionLabel) throws IOException {
    Files.createDirectories(jar.getParent());
    byte[] classBytes = compileLabelClass(versionLabel);
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      out.putNextEntry(new JarEntry("com/microjvm/tasks/versioned/Label.class"));
      out.write(classBytes);
      out.closeEntry();
    }
  }

  private static byte[] compileLabelClass(String versionLabel) throws IOException {
    Path scratch = Files.createTempDirectory("label-src");
    Path src = scratch.resolve("com/microjvm/tasks/versioned/Label.java");
    Files.createDirectories(src.getParent());
    Files.writeString(
        src,
        """
        package com.microjvm.tasks.versioned;
        public final class Label {
          public static String value() { return "%s"; }
        }
        """
            .formatted(versionLabel));
    Path classes = scratch.resolve("classes");
    Files.createDirectories(classes);
    Process javac =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                "-d",
                classes.toString(),
                src.toString())
            .inheritIO()
            .start();
    try {
      if (javac.waitFor() != 0) {
        throw new IOException("javac failed for Label " + versionLabel);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
    return Files.readAllBytes(classes.resolve("com/microjvm/tasks/versioned/Label.class"));
  }

  private static void jarDirectory(Path classesDir, Path jar) throws IOException {
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      try (var walk = Files.walk(classesDir)) {
        walk.filter(Files::isRegularFile)
            .forEach(
                file -> {
                  try {
                    String name = classesDir.relativize(file).toString().replace('\\', '/');
                    out.putNextEntry(new JarEntry(name));
                    Files.copy(file, out);
                    out.closeEntry();
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                });
      }
    }
  }
}
