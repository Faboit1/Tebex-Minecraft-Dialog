#!/bin/bash
sed -i 's/public CompletableFuture          createBan(String playerUUID, String ip, String reason)/@param reason the reason\\n    public CompletableFuture          createBan(String playerUUID, String ip, String reason)/' sdk/src/main/java/io/tebex/sdk/SDK.java
sed -i 's/public TebexRequest withBody(String body, String method)/@param method the method\\n    public TebexRequest withBody(String body, String method)/' sdk/src/main/java/io/tebex/sdk/request/TebexRequest.java
