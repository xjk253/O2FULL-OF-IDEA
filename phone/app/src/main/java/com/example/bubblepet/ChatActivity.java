package com.example.bubblepet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etChatInput;
    private Button btnSend;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private AiChatClient aiChatClient;
    private MessageStore messageStore;

    private final AiChatClient.OnSentenceListener sentenceListener = sentence -> {
        String text = sentence.display.isEmpty() ? sentence.ttsText : sentence.display;
        if (text.isEmpty()) return;
        messages.add(new ChatMessage(text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        // 持久化由 OverlayPetService 统一负责，避免重复保存
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // API 35+ 默认 edge-to-edge，需显式处理 IME insets
        // 用 WindowInsetsCompat 监听键盘高度，给根布局加底部 padding
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_chat);

        View root = findViewById(R.id.root_chat);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    Math.max(ime.bottom, systemBars.bottom)
            );
            return WindowInsetsCompat.CONSUMED;
        });

        rvChat = findViewById(R.id.rv_chat);
        etChatInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_send);

        aiChatClient = AiChatClient.getInstance(this);
        aiChatClient.connect();
        messageStore = new MessageStore(this);

        // 加载历史
        messages.addAll(messageStore.load());
        adapter = new ChatAdapter(messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);
        if (!messages.isEmpty()) {
            rvChat.scrollToPosition(messages.size() - 1);
        }

        // 接收每一句完整 sentence,逐条插入到聊天列表
        aiChatClient.addOnSentenceListener(sentenceListener);

        btnSend.setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(msg);
                etChatInput.setText("");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 不再强制弹键盘，避免布局跳变导致输入框位置不一致
        // 用户点击输入框时系统会自然弹出，adjustResize 会正确处理布局
    }

    private void sendMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        messageStore.append(messages.get(messages.size() - 1));

        // AI 回复通过 sentenceListener 逐条返回，无需在此回调
        aiChatClient.sendMessage(text, null);
    }

    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position < 0 || position >= messages.size()) return;
            ChatMessage msg = messages.get(position);
            holder.tvMessage.setText(msg.getText());
            holder.tvMessage.setMaxWidth((int) (holder.tvMessage.getResources().getDisplayMetrics().density * 280));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (msg.isUser()) {
                holder.tvMessage.setBackgroundResource(R.drawable.bg_bubble_user);
                holder.tvMessage.setTextColor(0xFF000000);
                lp.gravity = Gravity.END;
            } else {
                holder.tvMessage.setBackgroundResource(R.drawable.bg_bubble_ai);
                holder.tvMessage.setTextColor(0xFF000000);
                lp.gravity = Gravity.START;
            }
            // gravity 要设在 TextView 上，不是 itemView 上
            // itemView 的 LayoutParams 属于 RecyclerView 不支持 gravity
            holder.tvMessage.setLayoutParams(lp);
            // itemView 始终填满宽度，让 TextView 能在内部靠左或靠右
            holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessage;

            ViewHolder(View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tv_message);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiChatClient != null) {
            aiChatClient.removeOnSentenceListener(sentenceListener);
        }
        // 单例由所有组件共享，不在这里销毁
        aiChatClient = null;
    }
}
