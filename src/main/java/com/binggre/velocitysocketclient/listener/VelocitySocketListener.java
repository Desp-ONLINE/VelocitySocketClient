package com.binggre.velocitysocketclient.listener;

import com.binggre.velocitysocketclient.socket.SocketResponse;
import lombok.Setter;

@Setter
public abstract class VelocitySocketListener implements SocketListener {

    private SocketResponse latestResponse;

    @Override
    public SocketResponse getLatestResponse() {
        return latestResponse;
    }

}