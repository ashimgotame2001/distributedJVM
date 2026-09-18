package com.microjvm.node.artifact;

import com.microjvm.common.ArtifactCoordinates;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resolves jars from a local directory layout {@code root/group/name/version/*.jar} until the
 * Bytecode Fabric registry lands in Weeks 6–7.
 */
public final class LocalArtifactResolver {
  private final Path root;

  public LocalArtifactResolver(Path root) {
    this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
  }

  public Path root() {
    return root;
  }

  public List<Path> resolve(ArtifactCoordinates coordinates) throws IOException {
    Path dir =
        root.resolve(coordinates.group().replace('.', '/'))
            .resolve(coordinates.name())
            .resolve(coordinates.version());
    if (!Files.isDirectory(dir)) {
      throw new IOException("Artifact directory missing: " + dir);
    }
    List<Path> jars = new ArrayList<>();
    try (var stream = Files.list(dir)) {
      stream.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().forEach(jars::add);
    }
    if (jars.isEmpty()) {
      throw new IOException("No jars under " + dir);
    }
    return jars;
  }

  public Path artifactDir(ArtifactCoordinates coordinates) throws IOException {
    Path dir =
        root.resolve(coordinates.group().replace('.', '/'))
            .resolve(coordinates.name())
            .resolve(coordinates.version());
    Files.createDirectories(dir);
    return dir;
  }
}
