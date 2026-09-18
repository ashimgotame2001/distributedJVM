# MicroJVM — Phased Weekly Implementation Plan

2026-09-18 · @ashim.gotame

## Plan overview & assumptions

This is a 20-week, four-phase plan to build MicroJVM from an empty repo to a resilient, demoable distributed execution runtime. It operationalizes the seven milestones in the [execution-flow design](https://claude.ai/code/artifact/5623dc09-a78f-4b22-807f-5bb66207ce60): phases 1–3 build the spine, 4–5 make it intelligent and composable, 6–7 make it resilient.

**Working assumptions** (adjust and the week counts scale with you):

- **Team:** a small dedicated squad — 1 architect/tech lead, 2 backend engineers, and a part-time SRE/DevOps and QA. Roles are defined in the RACI section.
- **Cadence:** 1-week sprints, Monday planning and Friday demo. Every phase ends with a working, demoable slice — no phase is "integration at the end."
- **Stack:** Java 21, Spring Boot, gRPC + protobuf, Gradle, containerized nodes. A 3-node cluster (dev) is available from Week 1.
- **Estimates** are in ideal engineer-weeks for this team size; a solo build roughly doubles the calendar, a 6-engineer team compresses phases 1–3.

**Definition of Done (inherited by every task):** code merged behind review; unit + integration tests green in CI; the behavior is demonstrable on the 3-node dev cluster; telemetry/logging present where relevant; and the public contract (proto or SDK API) is documented. Individual tasks add their own acceptance in the "Done when" column.

## Phase map

Four phases, each ending on a gate that must pass before the next phase starts. The gate is the phase's real deliverable; everything in its weeks exists to reach it.

| Phase | Weeks | Milestones | Goal | Exit gate |
| --- | --- | --- | --- | --- |
| **0 — Foundations** | 1 | — | Repo, contracts, CI, 3-node cluster | A no-op task round-trips over gRPC between two nodes |
| **1 — Execution Spine** | 2–7 | M1–M3 | Portable execution: SDK → place (fixed) → fabric → run → result | A real task submitted from JVM-A runs on JVM-B via versioned artifacts, future resolves |
| **2 — Placement & Composition** | 8–12 | M4–M5 | Telemetry-driven placement; DAGs & parallelism | Runtime picks the best node from live metrics; a 5-task DAG runs with fan-in/out |
| **3 — Resilience** | 13–18 | M6–M7 | Failure recovery, checkpointing, migration | Kill a node mid-task → auto-recovers; drain a node → running task migrates |
| **4 — Hardening & GA** | 19–20 | — | Observability, load test, docs, release | Cluster survives a soak + chaos test; v1.0 tagged with SDK docs |

Phases 0–1 are strictly sequential (they build the contract everything else depends on). Phases 2 and 3 each have internal parallelism that the weekly tables call out.

## Team roles & RACI

The **Owner** column in every weekly table names the role accountable for the task. One role owns each task; others support.

| Role | Code | Owns | Also supports |
| --- | --- | --- | --- |
| Architect / Tech Lead | **TL** | Contracts, placement algorithm, design reviews | Everything (final sign-off) |
| Backend Engineer 1 | **BE1** | SDK, Task Manager, DAG engine | Executor, tests |
| Backend Engineer 2 | **BE2** | Node Executor, Bytecode Fabric, migration | SDK, tests |
| SRE / DevOps (part-time) | **SRE** | CI/CD, cluster, telemetry pipeline, chaos test | Load testing |
| QA (part-time) | **QA** | Integration & acceptance tests, soak test | Test data, demos |

Across the whole program the RACI holds constant: **TL** is *Accountable* for architecture and gate sign-off, engineers are *Responsible* for their components, **SRE/QA** are *Consulted* on testability and ops, and the product sponsor is *Informed* at each Friday demo. Weekly tables only show the single accountable Owner to stay readable.

## Phase 0 — Foundations (Week 1)

**Goal:** stand up the skeleton everything else plugs into, and prove two JVMs can talk. No business logic yet — this week exists so that from Week 2 every engineer works against a fixed contract and a real cluster.

### Week 1 — Repo, contracts & cluster

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Project & build scaffold | Teams need one build with clear module boundaries to avoid rework | Multi-module Gradle repo: `sdk`, `runtime`, `node`, `fabric`, `proto`, `common` | TL | `./gradlew build` green; modules import cleanly |
| Define gRPC contracts | The wire contract is the one thing all components share; it must be fixed first | `.proto` for `NodeExecutor` (`ExecuteTask`, `CancelTask`, `ReportHealth`) + core messages (`ExecuteRequest`, `TaskEvent`, `NodeSnapshot`) | TL | Protos compile; stubs generated for client + server |
| CI/CD pipeline | Every later task's DoD requires green CI; it can't be an afterthought | CI runs build + tests + lint on PR; publishes node container image | SRE | A PR blocks on red, merges on green |
| 3-node dev cluster | Distributed behavior can't be validated on one host | Containerized node app deployable to 3 nodes; service discovery config | SRE | 3 nodes register and are reachable |
| No-op round-trip spike | De-risk gRPC + serialization before real work depends on them | `ExecuteTask` that returns a constant, called node-to-node | BE2 | JVM-A invokes JVM-B, gets a reply over gRPC |

**Exit gate:** a no-op task round-trips between two nodes over gRPC in CI-built containers.

## Phase 1 — Portable Execution Spine (Weeks 2–7)

**Goal:** prove the hard idea — portable computation. By Week 7 a real task submitted from one JVM runs on another via exact versioned artifacts, and the caller's future resolves. Placement is faked (fixed target) until Phase 2; the value here is the spine, not the intelligence.

### Week 2 — SDK & task capture (M1)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| `MicroTask` / `MicroFuture` API | Developers need an ergonomic way to express remote work | Public SDK: define a task, submit, hold a typed future | BE1 | Sample app compiles against the API |
| Task descriptor + codec | Any node must reconstruct the task from bytes | Serializable descriptor (coords, entry, args, constraints) + pluggable codec | BE1 | Descriptor round-trips through serialize/deserialize |
| `submit` + task store | The runtime must track a task the moment it exists | `submit` accepts a descriptor, persists `CREATED`→`QUEUED`, returns future handle | BE1 | Submitted task is queryable by `taskId` |

### Week 3 — Runtime dispatch, fixed placement (M1)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Runtime coordinator + fixed placement | Need a dispatch path before scoring exists | Coordinator that assigns every task to a configured target node | TL | Task reaches `PLACED` on the target |
| gRPC dispatch client | The runtime must invoke the executor remotely | Client calls `ExecuteTask` with args + artifact refs; consumes the event stream | BE2 | Dispatch call succeeds against a live node |
| Result routing to future | The caller must receive the value or exception | Terminal event completes the `MicroFuture`; errors propagate as exceptions | BE1 | **End-to-end demo:** task on JVM-B, result on JVM-A |

### Week 4 — Node executor: classloading (M2)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Isolated classloader per task | Two artifact versions must coexist without collision | Child classloader scoped to a task's artifact set | BE2 | Two lib versions run in one JVM, no clash |
| Arg binding + entry invocation | The node must run the exact method the developer named | Deserialize args, resolve entry point, invoke | BE2 | A real computation returns a real value |
| Worker pool + exception wrapping | An uncaught error must not kill the node | Managed pool; failures become typed `TaskEvent`s | BE2 | A throwing task yields an error event, node stays up |

### Week 5 — Node executor: hardening (M2)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Idempotent dispatch by `taskId` | Retries and migration (later) must never double-run work | Executor dedups repeat `ExecuteTask` for a live/known `taskId` | BE2 | Re-sent dispatch runs once, returns cached result |
| Telemetry counters + health skeleton | Placement (Phase 2) depends on this feed existing early | Per-task counters on the stream; `ReportHealth` emits a basic snapshot | BE2 | Node streams CPU/heap/thread snapshot |
| Cancel + resource release | Stuck/aborted tasks must free slots | `CancelTask` stops work; classloader + slot released on completion | BE2 | Cancel frees the worker within budget |

### Week 6 — Bytecode Fabric: registry (M3)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Artifact Registry | Code can't rely on a shared filesystem across nodes | Content-addressed store keyed by `group:name:version` + hash | BE2 | Publish then fetch returns identical bytes |
| Immutable versioned coordinates | Caches must never go stale; nodes must agree on a version | Publish rejects re-publishing a changed coordinate | TL | Same coord always yields the same hash |
| Dependency closure resolution | A task needs its transitive deps, not just its jar | Resolve full closure from a dependency graph v1 | BE1 | Closure for a multi-dep task is correct |

### Week 7 — Bytecode Fabric: cache & gate (M3)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Per-node cache + `ensureArtifacts` | The common case must be a local hit, not a pull | Cache with hit/miss → fetch → verify hash → seed | BE2 | Second run on a node is a cache hit |
| Wire the `PLACED→RUNNING` gate | A task must never start without its code present | Dispatch blocks until `ensureArtifacts` confirms ready | TL | Missing-artifact task waits, then runs after pull |
| Phase-1 integration test | Prove the whole spine, not just parts | Automated test: submit → place → fabric → run → result on a cold node | QA | Green in CI on the 3-node cluster |

**Exit gate:** a real task submitted from JVM-A runs on JVM-B using versioned artifacts it did not previously have, and the future resolves with the correct value.

## Phase 2 — Intelligent Placement & Composition (Weeks 8–12)

**Goal:** replace the fixed target with real, telemetry-driven placement, and let tasks compose into DAGs. This is where MicroJVM stops being "remote method call" and becomes a scheduler. The telemetry feed from Phase 1 now has a consumer.

### Week 8 — Telemetry pipeline & node registry (M4)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Telemetry ingestion | Placement is only as good as its data | Runtime aggregates every node's `ReportHealth` stream | SRE | All 3 nodes' metrics visible to the runtime |
| Node registry (latest snapshot) | Scoring needs O(1) access to current node state | In-memory registry: newest `NodeSnapshot` per node, with staleness | TL | Registry reflects a change within one interval |
| Metrics dashboard | Operators (and demos) need to see cluster state | Basic Grafana/console view of CPU, heap, GC, tasks | SRE | Live cluster metrics render |

### Week 9 — Placement Engine v1 (M4)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Hardware hard-filter | A task must never land on a node that can't run it | Filter candidates by GPU/native/arch/heap requirements | TL | Ineligible nodes excluded from candidates |
| Weighted scoring function | The "best node" decision must be explainable and tunable | Normalized weighted score over CPU/heap/GC/load; config-driven weights | TL | Scores logged per decision, weights hot-reload |
| Replace fixed placement | The spine must now choose, not be told | Coordinator uses `argmax(score)`; fixed target removed | TL | Same task routes to different nodes as load shifts |

### Week 10 — Locality-aware placement & by-reference results (M4–M5)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Artifact-locality signal | Avoiding a pull is the biggest cheap win | Fabric reports who caches what; scoring prefers local nodes | BE2 | A node with the code is preferred, pull avoided |
| Result-by-reference | Large / chained results must not flow through the caller | Node keeps value locally, returns a handle; fetch-on-read | BE1 | A big result stays on-node until read |
| Data-locality hint | Consumers should run near their inputs | Placement reads handle location as a locality bonus | TL | Consumer placed on the producer's node when cheap |

### Week 11 — DAG engine (M5)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Dependency edges on futures | Real workloads are graphs, not single calls | SDK records an edge when a future is passed as an arg | BE1 | A task with a future arg registers a dependency |
| Dependency gating | A task must not run before its inputs exist | Task Manager holds `CREATED` until all upstreams resolve | BE1 | Downstream starts only after upstreams complete |
| Concurrent placement | Independent tasks should run in parallel automatically | Ready tasks with no pending edges are placed at once | TL | Two independent tasks run on two nodes concurrently |

### Week 12 — Composition & fan-in/out (M5)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Future passing without return | Intermediates shouldn't round-trip to the caller | A future feeds another task; value moves node-to-node | BE1 | Caller never receives an intermediate value |
| Fan-in / fan-out + backpressure | Graphs widen and narrow; the queue must not overrun | Join semantics; scheduler admission control under load | TL | A 1→N→1 graph completes correctly under load |
| Phase-2 integration test | Prove smart placement + DAG together | 5-task DAG on the cluster, placement decisions asserted | QA | Green in CI; decisions match expected nodes |

**Exit gate:** the runtime picks the best node from live metrics, and a 5-task DAG runs with correct fan-in/fan-out and measurable parallelism.

## Phase 3 — Resilience: recovery & migration (Weeks 13–18)

**Goal:** make the cluster survive node loss and rebalance running work. Everything here leans on the idempotent, `taskId`-keyed dispatch built in Phase 1 — recovery re-enters the same spine at `QUEUED`, it is not a separate pipeline.

### Week 13 — Failure detection (M6)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Heartbeat monitor | A dead node must be noticed fast | `ReportHealth` gap past a deadline marks a node suspect/down | SRE | Killed node detected within the SLA window |
| Task progress watchdog | A hung task must not block forever | No progress event past budget → task flagged stuck | BE1 | A sleeping task trips the watchdog |
| Failure classification | Different failures need different responses | Classify: node crash / fatal error / hang / degrade | TL | Each class raises the right event |

### Week 14 — Retry & re-placement (M6)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| `LOST` → re-place | A node's tasks must survive its death | Down node's tasks → `LOST` → back to `QUEUED`, re-placed | BE1 | Tasks on a killed node finish elsewhere |
| Retry policy + attempt cap | Retries must be bounded and declarable | Retryable flag, attempt counter, max-retries → `FAILED` | TL | Non-retryable fails fast; retryable retries N times |
| Idempotency verification | Re-execution must not duplicate side effects | Test suite proving `taskId` dedup across re-placement | QA | Re-placed task produces exactly one result |

### Week 15 — Checkpointing: capture (M7)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| `Checkpointable` interface | Long tasks must save progress, not restart | SDK contract for tasks to expose resumable state + marker | TL | A sample long task implements it |
| State snapshot in executor | The node must capture state safely | Pause task, serialize its checkpoint state, resume | BE2 | Snapshot taken with no state change mid-capture |
| Checkpoint policy | Snapshotting has a cost; it must be controlled | Interval / step-boundary triggers; opt-in per task | BE2 | Checkpoints occur on the configured trigger |

### Week 16 — Checkpointing: resume (M7)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Checkpoint store | State must outlive the node that made it | Durable/fabric-backed store keyed by `taskId` + marker | BE2 | Checkpoint survives its origin node's death |
| Resume-from-checkpoint | Recovery should continue, not restart | Executor resumes from marker instead of from zero | BE2 | Task resumes past the last checkpoint |
| Recovery uses checkpoints | Failure recovery should exploit checkpoints when present | `LOST` task with a checkpoint resumes; else re-runs | BE1 | Recovered long task skips completed work |

### Week 17 — Migration (M7)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Migration Manager | Running work must move to better/healthier nodes | Orchestrate pause → checkpoint → transfer → resume → discard | BE2 | A running task moves node A→B, future intact |
| Safe hand-off protocol | A mid-migration crash must not lose or double-run work | Source discarded only after target confirms `RUNNING` | TL | Crash mid-migration falls back to checkpoint |
| Node drain | Maintenance/degrade must not drop work | Drain: stop new tasks, migrate running ones off | SRE | Draining a node empties it with no failures |

### Week 18 — Resilience integration (M6–M7)

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Chaos test harness | Resilience claims must be proven, not asserted | Automated kill/slow/partition injection during a workload | SRE | Harness runs in CI against the cluster |
| Recovery + migration e2e | The two layers must work together under stress | Kill a node mid-DAG with checkpointed tasks; assert completion | QA | DAG completes correctly despite a node loss |
| Degrade-driven migration demo | Show adaptive rebalancing end to end | Load a node until its score decays → auto-migrate | TL | Running task migrates off the degrading node |

**Exit gate:** killing a node mid-task auto-recovers (from checkpoint where available), and draining a node migrates its running tasks with no lost or duplicated results.

## Phase 4 — Hardening, observability & GA (Weeks 19–20)

**Goal:** turn a working system into a shippable v1.0 — observable, load-proven, secured, and documented enough for a first real workload.

### Week 19 — Observability & performance

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Distributed tracing | Operators must debug a task across nodes | Trace/span per task across submit→place→run→result | SRE | One task's full journey is traceable |
| Load & soak test | v1 must hold up beyond demo scale | Sustained multi-hour load at target task rate | QA | Soak runs clean with no leaks or drift |
| Performance & weight tuning | Placement must be good, not just correct | Tune scoring weights + hot paths against load results | TL | Placement meets target utilization/latency |

### Week 20 — Security, docs & release

| Task | Business requirement | Expected execution & deliverable | Owner | Done when |
| --- | --- | --- | --- | --- |
| Node-to-node security | The fabric moves executable code; it must be trusted | mTLS between nodes + signed/verified artifacts | TL | Unsigned artifact or untrusted peer is rejected |
| SDK docs + samples | Adoption needs a clear on-ramp | Getting-started guide, API reference, runnable samples | BE1 | A new dev runs a task from docs alone |
| v1.0 release | A tagged, reproducible baseline to build on | Versioned release, changelog, deploy runbook | TL | v1.0 tagged; cluster deploys from the runbook |

**Exit gate:** the cluster survives a soak + chaos test at target scale, node-to-node traffic is secured, and v1.0 is tagged with SDK documentation.

## Cross-cutting workstreams

Four workstreams run continuously across all phases rather than as one-time tasks. They are not "phase 4 clean-up" — deferring them is the most common way this kind of project accrues unrecoverable debt.

| Workstream | Runs | Delivered continuously | Owner |
| --- | --- | --- | --- |
| Testing | Weeks 1–20 | Unit tests per PR; an integration test per phase gate; the chaos + soak suites from Phase 3 onward | QA |
| DevOps / infra | Weeks 1–20 | CI/CD, the dev cluster, container images, telemetry pipeline, deploy runbook | SRE |
| Documentation | Weeks 1–20 | Living proto/SDK reference kept current each week; getting-started guide finalized in Phase 4 | BE1 |
| Security | From Week 6 | Artifact integrity (hashing) from the Fabric onward; mTLS + signing hardened in Phase 4 | TL |

The rule of thumb: each weekly task's Definition of Done already folds in the test, the doc update, and the telemetry for that task, so these workstreams are mostly *coordination and the pieces that span tasks*, not separate backlogs.

## Risks, dependencies & exit criteria

The biggest risks are technical and front-loaded — they threaten the spine, so they are mitigated in Phases 0–3 by design, not deferred.

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Non-idempotent tasks double-run | Corrupt results under retry/migration | `taskId` dedup from M2; idempotency test suite (Week 14) gates Phase 3 |
| JVM state can't be snapshotted portably | Migration/checkpoint slips | Scope to `Checkpointable` tasks; short tasks re-run instead (Week 15) |
| Artifact/version drift across nodes | Wrong code runs; heisenbugs | Immutable content-hashed coords from M3; publish rejects mutation |
| Placement thrash / bad decisions | Poor utilization, hot nodes | Config-driven weights + hysteresis; tuned against load (Week 19) |
| Serialization incompatibility | Nodes can't decode args/results | One agreed codec fixed in Week 2; contract tests in CI |
| Team smaller than assumed | Timeline slips | Phases are independently demoable; drop Phase 3 scope before Phase 1 quality |

**External dependencies:** a provisioned 3-node dev cluster (Week 1), an artifact-store backend for the registry, and a metrics/tracing stack (Prometheus/Grafana or equivalent). Each is owned by SRE and needed before the phase that first uses it.

**Program exit / GA criteria — v1.0 ships when all hold:**

1. A DAG of tasks runs across the cluster with telemetry-driven placement.
2. Killing any single node mid-workload loses no task and no result.
3. A running task migrates off a draining or degrading node cleanly.
4. The cluster passes a multi-hour soak and a chaos test at target scale.
5. Node-to-node traffic and artifacts are authenticated and integrity-checked.
6. A new developer can write and run a distributed task from the SDK docs alone.
