package com.distributed.jvm.coordinator;


import com.distributed.jvm.core.utils.ServerConfigUtil;
import com.distributed.jvm.grpc.EmptyRequest;
import com.distributed.jvm.grpc.NodeAgentServiceGrpc;
import com.distributed.jvm.grpc.NodeHealthResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;

public class LoadBalancerApplication {

    private final NodeAgentServiceGrpc.NodeAgentServiceBlockingStub stub;

    public LoadBalancerApplication(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = NodeAgentServiceGrpc.newBlockingStub(channel);
    }

    public NodeHealthResponse checkNodeHealth() throws Exception {
        EmptyRequest request = EmptyRequest.newBuilder().build();
        return stub.getNodeHealth(request);
    }


    public static void main(String[] args) throws InterruptedException {

        ServerConfigUtil.loadPropertiesFromClasspath("application.properties");

        Map<String, Integer> hostPortMap = ServerConfigUtil.getHostPortMap();

        if (hostPortMap.isEmpty()) {
            System.out.println("❌ No host-port configurations found. Exiting.");
            return;
        }

        hostPortMap.forEach((host, port) -> {
            new Thread(() -> check(host, port), "NodeAgent-" + host + ":" + port).start();
            System.out.println("🔹 Starting NodeAgent at " + host + ":" + port);
        });

        System.out.println("✅ All NodeAgents are starting...");
        Thread.currentThread().join();

        ClusterManager.getAllRegisteredNodes().forEach(System.out::println);
    }

    private static void check(String host, int port) {
        LoadBalancerApplication application = new LoadBalancerApplication(host, port);
        try {
            NodeHealthResponse res = application.checkNodeHealth();
            ClusterManager.registerNodes(res);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}