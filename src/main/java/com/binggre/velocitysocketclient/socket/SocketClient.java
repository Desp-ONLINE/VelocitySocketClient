package com.binggre.velocitysocketclient.socket;

import com.binggre.binggreapi.functions.Callback;
import com.binggre.binggreapi.utils.file.FileManager;
import com.binggre.velocitysocketclient.listener.SocketListener;
import com.binggre.velocitysocketclient.listener.VelocitySocketListener;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {

    public static final String REQUEST = "Request:"; // 요청
    public static final String RESPONSE = "Response:"; // 응답
    public static final String ACKNOWLEDGMENT = "ResponsePort:"; // 승인

    private final Socket socket;
    @Getter
    private final int localPort;
    public final String SEPARATOR = "╊┞○@";
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final int portLength;
    private final Map<String, VelocitySocketListener> listeners = new HashMap<>();

    private final Map<String, Object> responseLocks = new HashMap<>(); // 이새끼 안해서 3시간 삽질
    private final Object responseLock = new Object(); // 이새끼 안해서 3시간 삽질

    @Getter
    private Thread thread;

    public SocketClient(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            localPort = socket.getLocalPort();
            portLength = String.valueOf(localPort).length();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() {
        Thread thread = new Thread(this::handleIncomingMessages);
        this.thread = thread;
        thread.start();
    }

    private void handleIncomingMessages() {
        while (true) {
            try {
                String read = reader.readLine();
                if (read == null) {
                    continue;
                }

                boolean isResponse = read.startsWith(RESPONSE);
                boolean isAcknowledgment = read.startsWith(ACKNOWLEDGMENT);
                String responsePort = extractResponsePort(read, isResponse, isAcknowledgment);

                if (isAcknowledgment && !String.valueOf(localPort).equals(responsePort)) {
                    continue;
                }

                read = cleanUpPrefix(read, isResponse, isAcknowledgment, responsePort);
                processMessage(read, isResponse, isAcknowledgment, responsePort);

            } catch (Exception ignored) {
            }
        }
    }

    private String extractResponsePort(String read, boolean isResponse, boolean isAcknowledgment) {
        if (isResponse) {
            return read.substring(RESPONSE.length(), RESPONSE.length() + portLength);
        } else if (isAcknowledgment) {
            return read.substring(ACKNOWLEDGMENT.length(), ACKNOWLEDGMENT.length() + portLength);
        }
        return null;
    }

    private String cleanUpPrefix(String read, boolean isResponse, boolean isAcknowledgment, String responsePort) {
        if (isResponse) {
            return read.replace(RESPONSE + responsePort, "");
        } else if (isAcknowledgment) {
            return read.replace(ACKNOWLEDGMENT + responsePort, "");
        }
        return read;
    }

    private void processMessage(String read, boolean isResponse, boolean isAcknowledgment, String responsePort) {
        for (VelocitySocketListener listener : listeners.values()) {
            if (!read.startsWith(listener.getId())) {
                continue;
            }

            if (isResponse) {
                String removedId = read.substring(listener.getId().length());
                String[] requestContents = removedId.split(SEPARATOR);
                SocketResponse response = listener.onRequest(requestContents);
                sendResponse(listener, responsePort, response);
            } else if (isAcknowledgment) {
                handleAcknowledgment(read, listener);
            } else {
                onlySendMessages(read, listener);
            }
            break;
        }
    }

    private void sendResponse(VelocitySocketListener socketListener, String port, SocketResponse response) {
        String json = FileManager.toJson(response);
        String key = ACKNOWLEDGMENT + port + socketListener.getId() + json;
        send(key);
    }

    private void handleAcknowledgment(String read, VelocitySocketListener listener) {
        String content = read.substring(listener.getId().length());
        SocketResponse response = FileManager.toObject(content, SocketResponse.class);
        listener.onResponse(response);
        listener.setLatestResponse(response);

        synchronized (responseLocks) {
            Object lock = responseLocks.remove(listener.getId());
            if (lock != null) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    private void onlySendMessages(String read, VelocitySocketListener listener) {
        String content = read.substring(listener.getId().length());
        String[] messages = content.split(SEPARATOR);
        listener.onReceive(messages);
    }

    public void requestAsync(Class<? extends VelocitySocketListener> socketListenerClass, @NotNull Callback<SocketResponse> responseCallback, String... requestContents) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            synchronized (this) {
                SocketResponse request = request(socketListenerClass);
                responseCallback.accept(request);
                executorService.shutdown();
            }
        });
    }

    @NotNull
    public SocketResponse request(Class<? extends VelocitySocketListener> socketListenerClass, String... requestContents) {
        String listenerKey = validateSocketListener(socketListenerClass);
        SocketListener listener = listeners.get(listenerKey);

        responseLocks.put(listener.getId(), responseLock);

        String requestContent = String.join(SEPARATOR, requestContents);
        send(REQUEST + listener.getId() + requestContent);

        synchronized (responseLock) {
            try {
                responseLock.wait();
            } catch (InterruptedException ignored) {
            }
        }

        return listener.getLatestResponse();
    }

    public void send(Class<? extends VelocitySocketListener> socketListenerClass, String... messages) {
        String listenerKey = validateSocketListener(socketListenerClass);
        SocketListener listener = listeners.get(listenerKey);
        String message = String.join(SEPARATOR, messages);
        send(listener.getId() + message);
    }

    public void send(String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (Exception ignored) {
        }
    }

    public void close() {
        listeners.clear();
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String validateSocketListener(Class<? extends VelocitySocketListener> socketListenerClass) {
        String listenerKey = socketListenerClass.getCanonicalName();
        if (!listeners.containsKey(listenerKey)) {
            throw new NullPointerException("등록되지 않은 소켓 클래스입니다. - " + listenerKey);
        }
        return listenerKey;
    }

    public void registerListener(Class<? extends VelocitySocketListener> socketListenerClass) {
        try {
            VelocitySocketListener listener = socketListenerClass.getConstructor().newInstance();
            listeners.put(socketListenerClass.getCanonicalName(), listener);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}