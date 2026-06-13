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
    private boolean connected = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 当前对话的回调，用于收集多句回复
    private OnResponseListener currentListener;
    private final StringBuilder replyBuffer = new StringBuilder();

    public AiChatClient() {
        connect();
    }

    private void connect() {
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
                    // 自动重连（必须新建 WebSocketClient，不可复用）
                    wsClient = null;
                    mainHandler.postDelayed(() -> {
                        if (!connected) connect();
                    }, 3000);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket 错误: " + ex.getMessage());
                }
            };
            wsClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "连接失败: " + e.getMessage());
        }
    }

    public void sendMessage(String message, OnResponseListener listener) {
        currentListener = listener;
        replyBuffer.setLength(0);

        if (wsClient != null && connected) {
            // 发送 JSON 格式消息
            String json = "{\"type\":\"chat\",\"text\":" + quote(message) + "}";
            wsClient.send(json);
        } else if (wsClient != null) {
            // 正在连接中，等一下再发
            mainHandler.postDelayed(() -> {
                if (wsClient != null && connected) {
                    String json = "{\"type\":\"chat\",\"text\":" + quote(message) + "}";
                    wsClient.send(json);
                } else if (listener != null) {
                    listener.onResponse("连接中，请稍后再试...");
                }
            }, 2000);
        } else {
            // 没有连接，新建
            connect();
            mainHandler.postDelayed(() -> {
                if (wsClient != null && connected) {
                    String json = "{\"type\":\"chat\",\"text\":" + quote(message) + "}";
                    wsClient.send(json);
                } else if (listener != null) {
                    listener.onResponse("连接中，请稍后再试...");
                }
            }, 3000);
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
                    // 收集每句回复
                    String text = msg.optString("text", "");
                    if (!text.isEmpty()) {
                        if (replyBuffer.length() > 0) replyBuffer.append("\n");
                        replyBuffer.append(text);
                    }
                    break;

                case "done":
                    // 本轮对话结束，回调完整回复
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

    /** 简易 JSON 字符串转义 */
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
        if (wsClient != null) {
            try { wsClient.close(); } catch (Exception ignored) {}
        }
    }
}
