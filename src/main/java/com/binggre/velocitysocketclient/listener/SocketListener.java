package com.binggre.velocitysocketclient.listener;

import com.binggre.velocitysocketclient.socket.SocketResponse;
import org.jetbrains.annotations.NotNull;

public interface SocketListener {

    void onReceive(String[] messages);

    @NotNull SocketResponse onRequest(String... requestContents);

    void onResponse(SocketResponse response);

    SocketResponse getLatestResponse();

    default String getId() {
        return getClass().getCanonicalName();
    }
}