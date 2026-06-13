package com.example.bubblepet;

import android.os.Handler;
import android.os.Looper;

public class AiChatClient {

    public interface OnResponseListener {
        void onResponse(String reply);
    }

    private static final String[] MOCK_REPLIES = {
            "你好呀~我是你的小气泡宠物！",
            "今天天气真不错呢~",
            "摸摸我~我会开心的！",
            "你有什么想聊的吗？",
            "嘿嘿，我饿了...",
            "主人别走，陪我玩~",
            "我是一个可爱的气泡宠物！",
            "你在忙什么呀？"
    };

    private int lastReplyIndex = -1;

    public void sendMessage(String message, OnResponseListener listener) {
        new Thread(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String reply = getRandomReply();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (listener != null) {
                    listener.onResponse(reply);
                }
            });
        }).start();
    }

    private String getRandomReply() {
        int index;
        do {
            index = (int) (Math.random() * MOCK_REPLIES.length);
        } while (index == lastReplyIndex && MOCK_REPLIES.length > 1);
        lastReplyIndex = index;
        return MOCK_REPLIES[index];
    }
}
