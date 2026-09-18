# MicroJVM

Distributed JVM execution runtime (Gradle multi-module).

## Modules

| Module | Role |
| --- | --- |
| `proto` | gRPC `NodeExecutor` contracts |
| `common` | Shared types, descriptor, codec SPI |
| `sdk` | `MicroTask` / `MicroFuture` public API |
| `runtime` | Submit, fixed placement, gRPC dispatch |
| `node` | Spring Boot gRPC executor (isolated classloaders) |
| `fabric` | Bytecode fabric (placeholder) |
| `samples-tasks` | Example task jar loaded by nodes |
| `samples` | Sample submit app |

The earlier Maven prototype under `distributed-jvm/` is reference-only.

## Build

```bash
./gradlew build
```

## Sample submit

Store-only (no node):

```bash
./gradlew :samples:run
```

Dispatch to a live node:

```bash
MICROJVM_TARGET_HOST=127.0.0.1 MICROJVM_TARGET_PORT=9091 ./gradlew :samples:run
```

## 3-node cluster + gRPC spike

```bash
./docker/smoke.sh
```
