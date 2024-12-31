package com.binggre.velocitysocketclient.listener;

import com.binggre.velocitysocketclient.socket.SocketResponse;

public interface SocketListener {

    void onReceive(String[] messages);

    SocketResponse onRequest();

    void onResponse(SocketResponse response);

    SocketResponse getLatestResponse();

    default String getId() {
        return getClass().getCanonicalName();
    }
}