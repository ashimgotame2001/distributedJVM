package com.distributed.jvm.coordinator;

import com.distributed.jvm.coordinator.models.RegisteredNodeInfo;
import com.distributed.jvm.grpc.NodeHealthResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ClusterManager {


    private static final List<RegisteredNodeInfo> registeredNodeInfos = new ArrayList<>();


    public static synchronized void registerNodes(NodeHealthResponse nodeHealthResponse) {
        registeredNodeInfos.add(new RegisteredNodeInfo(nodeHealthResponse.getNodeId(), nodeHealthResponse, nodeHealthResponse.getStatus()));
    }

    public static synchronized List<RegisteredNodeInfo> getAllRegisteredNodes() {
        System.out.println("📋 Returning nodes: " + registeredNodeInfos);
        return registeredNodeInfos;
    }

//    public static NodeHealthResponse getBestNode() {
//        // Example: pick node with lowest CPU load
//        return registeredNodeInfos.stream()
//                .min(Comparator.comparingDouble(n -> n.getCpu().getCpuLoadPercent()))
//                .orElse(null);
//    }


}
