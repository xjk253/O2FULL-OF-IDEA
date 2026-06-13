package com.example.bubblepet;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class AiChatClient {

    private static final String TAG = "AiChatClient";
    private static final String WS_URL = "ws://118.25.186.221:8080";

    public interface OnResponseListener {
        void onResponse(String reply);
    }

    private WebSocketClient wsClient;
    private String sessionId;
    private volatile boolean connected = false;
    private volatile boolean closed = false;  // 标记是否已主动关闭
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private OnResponseListener currentListener;
    private final StringBuilder replyBuffer = new StringBuilder();

    public AiChatClient() {
        connect();
    }

    private void connect() {
        if (closed) return;
        try {
            URI uri = new URI(WS_URL);
            wsClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected = true;
                    Log.d(TAG, "WebSocket 已连接");
                }

                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "收到消息: " + message);
                    handleServerMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    Log.d(TAG, "WebSocket 断开: " + reason);
                    if (!closed) {
                        // 延迟重连，用新对象
                        mainHandler.postDelayed(() -> {
                            if (!closed && !connected) {
                                try { connect(); } catch (Exception ignored) {}
                            }
                        }, 3000);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket 错误: " + ex.getMessage());
                }
            };
            wsClient.connectBlocking();  // 阻塞式连接，避免并发问题
        } catch (Exception e) {
            Log.e(TAG, "连接失败: " + e.getMessage());
            // 连接失败，延迟重试
            if (!closed) {
                mainHandler.postDelayed(() -> {
                    if (!closed && !connected) {
                        try { connect(); } catch (Exception ignored) {}
                    }
                }, 5000);
            }
        }
    }

    public void sendMessage(String message, OnResponseListener listener) {
        if (closed) return;
        currentListener = listener;
        replyBuffer.setLength(0);

        if (connected && wsClient != null && wsClient.isOpen()) {
            String json = "{\"type\":\"chat\",\"text\":" + quote(message) + "}";
            wsClient.send(json);
        } else {
            // 异步等待连接后发送
            new Thread(() -> {
                try {
                    // 已关闭的 client 不可复用，必须新建
                    if (wsClient == null || wsClient.isClosed()) {
                        if (!closed) connect();
                    }
                    // 等待连接建立
                    int wait = 0;
                    while (!connected && wait < 5000 && !closed) {
                        Thread.sleep(100);
                        wait += 100;
                    }
                    if (connected && wsClient != null && wsClient.isOpen()) {
                        String json = "{\"type\":\"chat\",\"text\":" + quote(message) + "}";
                        wsClient.send(json);
                    } else if (listener != null) {
                        mainHandler.post(() -> listener.onResponse("连接失败，请重试..."));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "发送失败: " + e.getMessage());
                    if (listener != null) {
                        mainHandler.post(() -> listener.onResponse("连接失败，请重试..."));
                    }
                }
            }).start();
        }
    }

    private void handleServerMessage(String raw) {
        try {
            org.json.JSONObject msg = new org.json.JSONObject(raw);
            String type = msg.optString("type", "");

            switch (type) {
                case "hello":
                    sessionId = msg.optString("sessionId", "");
                    Log.d(TAG, "Session: " + sessionId);
                    break;

                case "sentence":
                    String text = msg.optString("text", "");
                    if (!text.isEmpty()) {
                        if (replyBuffer.length() > 0) replyBuffer.append("\n");
                        replyBuffer.append(text);
                    }
                    break;

                case "done":
                    final String fullReply = replyBuffer.toString().trim();
                    if (currentListener != null && !fullReply.isEmpty()) {
                        mainHandler.post(() -> currentListener.onResponse(fullReply));
                    }
                    replyBuffer.setLength(0);
                    break;

                case "error":
                    String errMsg = msg.optString("message", "未知错误");
                    if (currentListener != null) {
                        mainHandler.post(() -> currentListener.onResponse("出错了: " + errMsg));
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "解析消息失败: " + e.getMessage());
        }
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    public void disconnect() {
        closed = true;
        connected = false;
        if (wsClient != null) {
            try { wsClient.close(); } catch (Exception ignored) {}
            wsClient = null;
        }
    }
}
