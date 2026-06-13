package com.example.bubblepet;

public class ChatMessage {

    private final String text;
    private final boolean isUser;
    private final long timestamp;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
