package com.binggre.velocitysocketclient.socket;

import com.binggre.velocitysocketclient.VelocityClient;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class SocketResponse {

    private final int serverPort;
    private final int socketPort;
    private final String[] messages;

    public SocketResponse(String[] messages) {
        this.serverPort = Bukkit.getServer().getPort();
        this.socketPort = VelocityClient.getInstance().getConnectClient().getLocalPort();
        this.messages = messages;
    }

    public boolean isEmpty() {
        return messages == null || messages.length == 0;
    }

    public static SocketResponse ok(String... messages) {
        return new SocketResponse(messages);
    }
}