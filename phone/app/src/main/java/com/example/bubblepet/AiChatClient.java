package com.example.bubblepet;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class AiChatClient {

    private static final String TAG = "BubblePet";
    private static final String PREFS_NAME = "bubblepet";
    private static final String KEY_GATEWAY_URL = "gateway_url";

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

    public interface OnSentenceListener {
        void onSentence(Sentence sentence);
    }

    public static class Sentence {
        public final String display;
        public final String ttsText;
        public final String expression;

        public Sentence(String display, String ttsText, String expression) {
            this.display = display;
            this.ttsText = ttsText;
            this.expression = expression;
        }
    }

    private final OkHttpClient client;
    private final String serverUrl;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile WebSocket webSocket;
    private volatile boolean isConnected = false;
    private OnConnectionListener connectionListener;
    private final List<OnSentenceListener> sentenceListeners = new CopyOnWriteArrayList<>();
    private volatile OnResponseListener pendingListener;
    private volatile String pendingMessage;
    private int lastFallbackIndex = -1;

    private static volatile AiChatClient instance;

    public static AiChatClient getInstance(Context context) {
        if (instance == null) {
            synchronized (AiChatClient.class) {
                if (instance == null) {
                    instance = new AiChatClient(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public AiChatClient(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedUrl = prefs.getString(KEY_GATEWAY_URL, null);
        this.serverUrl = savedUrl != null ? savedUrl : context.getString(R.string.gateway_url);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();
    }

    public synchronized void connect() {
        if (isConnected && webSocket != null) {
            return;
        }
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

    public void addOnSentenceListener(OnSentenceListener listener) {
        if (listener != null && !sentenceListeners.contains(listener)) {
            sentenceListeners.add(listener);
        }
    }

    public void removeOnSentenceListener(OnSentenceListener listener) {
        sentenceListeners.remove(listener);
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

    private static String stripTags(String s) {
        if (s == null) return "";
        return s
                .replaceAll("<think\\b[^>]*>[\\s\\S]*?</think\\b[^>]*>", "")
                .replaceAll("<think\\b[^>]*/>", "")
                .replaceAll("\\[[a-zA-Z_]+\\]", "")
                .trim();
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
                    String ttsText = json.optString("text", "");
                    String displayRaw = json.optString("display", "");
                    String expression = json.isNull("expression") ? null
                            : json.optString("expression", null);

                    // 显示文本优先用 display,缺失时回退到 text;剥离 [tag] 与 <think>
                    String display = stripTags(displayRaw.isEmpty() ? ttsText : displayRaw);
                    String cleanTts = stripTags(ttsText);
                    if (display.isEmpty() && cleanTts.isEmpty()) return;

                    final Sentence sentence = new Sentence(display, cleanTts, expression);
                    mainHandler.post(() -> {
                        for (OnSentenceListener l : sentenceListeners) {
                            l.onSentence(sentence);
                        }
                        if (pendingListener != null) {
                            pendingListener.onResponse(display.isEmpty() ? cleanTts : display);
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
