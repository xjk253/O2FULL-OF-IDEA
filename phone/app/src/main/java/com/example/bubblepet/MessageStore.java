package com.example.bubblepet;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 用 SharedPreferences + JSON 数组持久化聊天历史。
 * 选择此方案而非 Room 是为了不引入新依赖、保持项目轻量。
 *
 * 存储上限 100 条，超出后裁掉最早的部分。
 */
public class MessageStore {

    private static final String PREFS_NAME = "bubblepet";
    private static final String KEY_HISTORY = "chat_history";
    private static final int MAX_MESSAGES = 100;

    private final SharedPreferences prefs;

    public MessageStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<ChatMessage> load() {
        List<ChatMessage> list = new ArrayList<>();
        String raw = prefs.getString(KEY_HISTORY, null);
        if (raw == null) return list;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String text = o.getString("text");
                boolean isUser = o.getBoolean("isUser");
                long ts = o.optLong("ts", System.currentTimeMillis());
                list.add(new ChatMessage(text, isUser, ts));
            }
        } catch (JSONException e) {
            // 损坏数据直接丢弃，避免反复崩溃
        }
        return list;
    }

    public void save(List<ChatMessage> messages) {
        JSONArray arr = new JSONArray();
        int start = Math.max(0, messages.size() - MAX_MESSAGES);
        for (int i = start; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            JSONObject o = new JSONObject();
            try {
                o.put("text", m.getText());
                o.put("isUser", m.isUser());
                o.put("ts", m.getTimestamp());
                arr.put(o);
            } catch (JSONException e) {
                // 单条失败跳过
            }
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
    }

    public void append(ChatMessage message) {
        List<ChatMessage> list = load();
        list.add(message);
        save(list);
    }

    public void clear() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
