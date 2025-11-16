package com.distributed.jvm.core.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ServerConfigUtil {

    public static final Properties properties = new Properties();
    private static final Map<String, Integer> hostPortMap = new HashMap<>();

    public static void loadPropertiesFromClasspath(String resourcePath) {
        try (InputStream input = ServerConfigUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                System.err.println("❌ Properties not found in classpath: " + resourcePath);
                return;
            }
            properties.load(input);
            System.out.println("✅ Config loaded from classpath: " + resourcePath);
            loadHostPortMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void loadHostPortMap() {
        hostPortMap.clear();
        properties.forEach((keyObj, valueObj) -> {
            String key = keyObj.toString();
            if (key.startsWith("server.open.host.")) {
                String suffix = key.substring("server.open.host.".length());
                String host = valueObj.toString();
                String portKey = "server.open.port." + suffix;
                String portValue = properties.getProperty(portKey);

                if (portValue != null) {
                    try {
                        int port = Integer.parseInt(portValue);
                        hostPortMap.put(host, port);
                        System.out.println("🔍 Loaded host-port: " + host + ":" + port);
                    } catch (NumberFormatException e) {
                        System.err.println("❌ Invalid port for " + host + ": " + portValue);
                    }
                } else {
                    System.out.println("⚠️ Port not defined for host: " + host);
                }
            }
        });
    }

    public static Map<String, Integer> getHostPortMap() {
        return hostPortMap;
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String getFallbackPort() {
        return properties.getProperty("server.port");
    }

    public static String getFallbackHost() {
        return properties.getProperty("server.host");
    }
}

