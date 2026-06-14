package com.example.bubblepet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 当前会话的消息列表（内存）。
 * 生命周期：启动宠物时 clear，关闭宠物后仍保留直到下次启动。
 * ChatActivity 和 OverlayPetService 共享同一份数据。
 */
public final class SessionMessages {

    private static final List<ChatMessage> messages = new ArrayList<>();

    private SessionMessages() {}

    public static List<ChatMessage> get() {
        return Collections.unmodifiableList(messages);
    }

    public static void add(ChatMessage message) {
        messages.add(message);
    }

    public static void clear() {
        messages.clear();
    }

    public static int size() {
        return messages.size();
    }
}
