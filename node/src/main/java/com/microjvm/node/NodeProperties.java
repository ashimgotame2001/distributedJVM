package com.microjvm.node;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microjvm.node")
public class NodeProperties {
  private String id = "node-local";
  private String peerHost = "";
  private int peerPort = 0;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPeerHost() {
    return peerHost;
  }

  public void setPeerHost(String peerHost) {
    this.peerHost = peerHost;
  }

  public int getPeerPort() {
    return peerPort;
  }

  public void setPeerPort(int peerPort) {
    this.peerPort = peerPort;
  }

  public boolean hasPeer() {
    return peerHost != null && !peerHost.isBlank() && peerPort > 0;
  }
}
