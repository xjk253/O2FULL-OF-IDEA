package com.example.bubblepet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class AiChatClient {

    private static final String TAG = "BubblePet";

    private static final String[] FALLBACK_REPLIES = {
            "你好呀~我是你的小气泡宠物！",
            "今天天气真不错呢~",
            "摸摸我~我会开心的！",
            "你有什么想聊的吗？",
            "嘿嘿，我饿了...",
            "主人别走，陪我玩~",
            "我是一个可爱的气泡宠物！",
            "你在忙什么呀？"
    };

    public interface OnResponseListener {
        void onResponse(String reply);
    }

    public interface OnConnectionListener {
        void onConnectionChanged(boolean connected);
    }

    private final OkHttpClient client;
    private final String serverUrl;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile WebSocket webSocket;
    private volatile boolean isConnected = false;
    private OnConnectionListener connectionListener;
    private volatile OnResponseListener pendingListener;
    private volatile String pendingMessage;
    private int lastFallbackIndex = -1;

    public AiChatClient(Context context) {
        this.serverUrl = context.getString(R.string.gateway_url);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public void connect() {
        Log.d(TAG, "AiChatClient connecting to: " + serverUrl);
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();
        webSocket = client.newWebSocket(request, new BubbleWebSocketListener());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "disconnect");
            webSocket = null;
        }
        isConnected = false;
        notifyConnectionChanged(false);
    }

    public void sendMessage(String message, OnResponseListener listener) {
        Log.d(TAG, "sendMessage: " + message + ", isConnected: " + isConnected);
        if (isConnected && webSocket != null) {
            pendingListener = listener;
            try {
                JSONObject json = new JSONObject();
                json.put("type", "chat");
                json.put("text", message);
                webSocket.send(json.toString());
            } catch (JSONException e) {
                fallbackReply(listener);
            }
        } else {
            connect();
            pendingListener = listener;
            pendingMessage = message;
            new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isConnected) {
                    fallbackReply(listener);
                }
            }, 3000);
        }
    }

    public void setOnConnectionListener(OnConnectionListener listener) {
        this.connectionListener = listener;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void fallbackReply(OnResponseListener listener) {
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String reply = getFallbackReply();
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onResponse(reply);
                }
            });
        }).start();
    }

    private String getFallbackReply() {
        int index;
        do {
            index = (int) (Math.random() * FALLBACK_REPLIES.length);
        } while (index == lastFallbackIndex && FALLBACK_REPLIES.length > 1);
        lastFallbackIndex = index;
        return FALLBACK_REPLIES[index];
    }

    private void notifyConnectionChanged(boolean connected) {
        if (connectionListener != null) {
            mainHandler.post(() -> connectionListener.onConnectionChanged(connected));
        }
    }

    private class BubbleWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Log.d(TAG, "WebSocket onOpen, connected");
            isConnected = true;
            notifyConnectionChanged(true);
            if (pendingListener != null && pendingMessage != null) {
                String msg = pendingMessage;
                pendingMessage = null;
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "chat");
                    json.put("text", msg);
                    webSocket.send(json.toString());
                } catch (JSONException e) {
                    fallbackReply(pendingListener);
                }
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.d(TAG, "WebSocket onMessage: " + text);
            try {
                JSONObject json = new JSONObject(text);
                String type = json.optString("type");

                if ("sentence".equals(type)) {
                    String raw = json.optString("text", "");
                    final String sentenceText = raw.replaceAll("<think[^>]*>[\\s\\S]*?</think>", "")
                            .replaceAll("\\[[a-zA-Z_]+\\]", "").trim();
                    if (sentenceText.isEmpty()) return;
                    mainHandler.post(() -> {
                        if (pendingListener != null) {
                            pendingListener.onResponse(sentenceText);
                        }
                    });
                } else if ("done".equals(type)) {
                    pendingListener = null;
                } else if ("error".equals(type)) {
                    String errorMsg = json.optString("message", "未知错误");
                    mainHandler.post(() -> {
                        if (pendingListener != null) {
                            pendingListener.onResponse("[错误] " + errorMsg);
                            pendingListener = null;
                        }
                    });
                }
            } catch (JSONException e) {
                // ignore malformed messages
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            // not used
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(1000, null);
            isConnected = false;
            notifyConnectionChanged(false);
            scheduleReconnect();
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.e(TAG, "WebSocket onFailure: " + t.getMessage(), t);
            isConnected = false;
            notifyConnectionChanged(false);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            mainHandler.post(this::connect);
        }).start();
    }

    public void destroy() {
        disconnect();
    }
}
