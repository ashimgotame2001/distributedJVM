# MicroJVM

Distributed JVM execution runtime (Gradle multi-module).

## Modules

| Module | Role |
| --- | --- |
| `proto` | gRPC `NodeExecutor` contracts |
| `common` | Shared types, descriptor, codec SPI |
| `sdk` | `MicroTask` / `MicroFuture` public API |
| `runtime` | Submit + in-memory task store |
| `node` | Spring Boot gRPC node process |
| `fabric` | Bytecode fabric (placeholder) |
| `samples` | Sample submit app |

The earlier Maven prototype under `distributed-jvm/` is reference-only.

## Build

```bash
./gradlew build
```

## Sample submit

```bash
./gradlew :samples:run
```

## 3-node cluster + gRPC spike

```bash
./docker/smoke.sh
```

Or manually:

```bash
docker compose -f docker/docker-compose.yml up --build
```
