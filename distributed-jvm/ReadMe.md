# 🧩 Distributed JVM

The **Distributed JVM** project is a multi-node Java system built using **gRPC** for inter-node communication.
It provides the foundation for distributed computation, load balancing, and fault-tolerant task execution across multiple JVM instances.

---

## 🧱 Module Overview

### **1. Core**

* Contains all configuration files and global constants used across the project.
* Manages gRPC port bindings, cluster initialization, and shared settings.
* Acts as the backbone for system coordination.

### **2. Shared**

* Holds reusable code, DTOs, and utilities needed by all modules.
* Defines shared resources like task definitions, node models, and constants.
* Ensures type-safety and code consistency across the distributed system.

### **3. Client**

* Entry point for users or external systems.
* Submits tasks to the distributed JVM cluster and monitors task progress.
* Handles retries, responses, and client-side load balancing.

### **4. Node Agents**

* Represents the compute nodes (workers) in the cluster.
* Executes incoming tasks, reports load status, and handles task lifecycle.
* Supports concurrent task processing with `AtomicInteger`-based load tracking.

---

## 💡 Future Enhancements

1. **Dynamic Load Balancing** — Distribute tasks based on real-time CPU, memory, or historical usage metrics.
2. **Fault Tolerance** — Automatically reschedule tasks when a node fails.
3. **Cluster Coordinator** — A central controller to manage node registration and task assignment.
4. **Task Prioritization** — Queue tasks based on priority levels.
5. **Node Auto-Scaling** — Dynamically spawn or remove nodes based on workload.
6. **TLS Communication** — Secure all gRPC channels between nodes and clients.
7. **Monitoring Dashboard** — Real-time metrics visualization using Prometheus + Grafana.
8. **Distributed Cache Layer** — Cache results or intermediate data shared across nodes.
9. **Custom Scheduler Plugins** — Support different scheduling strategies (e.g., Round-Robin, Least-Loaded).
10. **Persistent Task Queue** — Use a message broker (like Kafka or RabbitMQ) for durability and reliability.

---

## 🚀 Getting Started

### 1. Prerequisites

Make sure you have:

* **Java 17+**
* **Maven 3.8+**
* **Bash shell** (for build script)
* **gRPC tools** (if not auto-generated via Maven)

---

### 2. Clone the Repository

```bash
git clone <repository-url>
cd distributed-jvm
```

---

### 3. Build All Modules

The project includes a helper script to build everything in the correct dependency order.
You can find it in the root directory as `build-all.sh`.

#### Run the Script

```bash
chmod +x build-all.sh
./build-all.sh
```

#### 🧰 Script Contents

```bash
#!/bin/bash
# build-all.sh
# A script to clean, compile, and install all modules for Distributed JVM project

set -euo pipefail  # Exit on error, unset variable, or failed pipe

PROJECT_ROOT=$(pwd)

echo "=============================="
echo "Cleaning and building all modules..."
echo "Project root: $PROJECT_ROOT"
echo "=============================="

# Function to build a module
build_module() {
    local module_name=$1
    echo "------------------------------"
    echo "Building $module_name module..."
    cd "$PROJECT_ROOT/$module_name"
    mvn clean install -U
    echo "$module_name module built successfully."
    echo "------------------------------"
}

# Build order matters due to dependencies
build_module "grpc"        # Generates gRPC classes
build_module "shared"      # Shared code used by others
build_module "core"        # Core logic
build_module "node-agent"  # Node agent module
build_module "client"      # Client module

echo "=============================="
echo "All modules built successfully!"
echo "=============================="
```

---

### 4. Run the Project

#### Start Node Agents

Each **Node Agent** acts as a worker node in the cluster:

```bash
cd node-agent/target
java -jar node-agent.jar
```

You can launch multiple nodes on different machines or ports.

#### Start the Client

Submit and manage distributed tasks:

```bash
cd client/target
java -jar client.jar
```

#### Configure

Edit configurations inside the **Core** module (e.g., `application.properties` or `config.yaml`):

* Node list and ports
* Cluster name
* Logging and monitoring options

---

### 5. Verify Cluster Health

Once started:

* Nodes will announce themselves to the cluster.
* The client discovers available nodes.
* Tasks are dispatched to the least-loaded node.

---

### ✅ You’re Ready

Your **Distributed JVM** cluster is now ready to handle distributed workloads!

---

## 🧠 Maintainers

**Author:** Ashim Gotame
**License:** MIT (or your preferred license)
**Version:** 0.1.0 — Initial Development
