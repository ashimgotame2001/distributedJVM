package com.distributed.jvm.agent;

import com.distributed.jvm.agent.service.NodeHealthService;
import com.distributed.jvm.core.utils.ServerConfigUtil;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeAgentApplication {

    public static void main(String[] args) {
        // 🔹 Load configuration
        ServerConfigUtil.loadPropertiesFromClasspath("application.properties");
        Map<String, Integer> hostPortMap = ServerConfigUtil.getHostPortMap();

        if (hostPortMap.isEmpty()) {
            System.out.println("❌ No host-port configurations found. Exiting.");
            return;
        }

        System.out.println("🧠 Initializing Node Agents from configuration...");

        // 🔹 Keep references to all running servers
        List<Server> servers = new ArrayList<>();

        // gRPC servers are non-blocking — can start multiple in same process
        hostPortMap.forEach((host, port) -> {
            try {
                String nodeId = UUID.randomUUID().toString();
                System.out.printf("🔹 Starting NodeAgent [ID=%s] on %s:%d%n", nodeId, host, port);

                Server server = ServerBuilder
                        .forPort(port)
                        .addService(new NodeHealthService(nodeId, host, port))
                        .executor(Executors.newFixedThreadPool(4)) // small pool per node
                        .build()
                        .start();

                servers.add(server);

                System.out.printf("✅ NodeAgent running on %s:%d (NodeId=%s)%n", host, port, nodeId);
            } catch (IOException e) {
                System.err.printf("❌ Failed to bind NodeAgent on %s:%d — %s%n", host, port, e.getMessage());
            }
        });

        System.out.println("✅ All NodeAgents are running.");

        // 🔹 Add graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down all NodeAgents...");
            servers.forEach(Server::shutdown);
        }));

        // 🔹 Block main thread to keep all servers alive
        try {
            for (Server server : servers) {
                server.awaitTermination();
            }
        } catch (InterruptedException e) {
            System.err.println("⚠️ NodeAgentApplication interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}
