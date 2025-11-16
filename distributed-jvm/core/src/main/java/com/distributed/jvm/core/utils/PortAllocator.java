package com.distributed.jvm.core.utils;

import java.util.HashMap;
import java.util.Map;

public class PortAllocator {

    public static Map<String, Integer> getAvailablePorts() {
        Map<String, Integer> hostPortMap = new HashMap<>();

        ServerConfigUtil.getHostPortMap().forEach((keyObj, valueObj) -> {
            String key = keyObj.toString();
            if (key.startsWith("server.open.host.")) {
                String suffix = key.substring("server.open.host.".length());
                String host = valueObj.toString();
                String portKey = "server.open.port." + suffix;
                String portValue = ServerConfigUtil.get(portKey);

                if (portValue != null) {
                    try {
                        int port = Integer.parseInt(portValue);
                        hostPortMap.put(host, port);
                        System.out.println("🔍 Loaded host-port: " + host + ":" + port);
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid port number for host " + host + ": " + portValue);
                    }
                } else {
                    System.out.println("⚠️ Port not defined for host: " + host);
                }
            }
        });

        return hostPortMap;
    }
}
