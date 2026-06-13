package com.example.bubblepet;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etChatInput;
    private Button btnSend;
    private TextView tvStatus;
    private ChatAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();
    private AiChatClient aiChatClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rvChat = findViewById(R.id.rv_chat);
        etChatInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_send);
        tvStatus = findViewById(R.id.tv_status);
        ImageView btnBack = findViewById(R.id.btn_back);

        aiChatClient = new AiChatClient();

        adapter = new ChatAdapter(messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) {
                sendMessage(msg);
                etChatInput.setText("");
            }
        });

        // 连接状态提示
        aiChatClient.setStatusListener(connected -> runOnUiThread(() -> {
            tvStatus.setText(connected ? "在线" : "连接中...");
            tvStatus.setTextColor(getResources().getColor(
                    connected ? R.color.chat_time_text : R.color.chat_send_btn,
                    getTheme()));
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        etChatInput.postDelayed(() -> {
            etChatInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etChatInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiChatClient != null) {
            aiChatClient.disconnect();
        }
    }

    private void sendMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);

        setStatusThinking();

        aiChatClient.sendMessage(text, reply -> {
            clearThinking();
            messages.add(new ChatMessage(reply, false));
            adapter.notifyItemInserted(messages.size() - 1);
            rvChat.scrollToPosition(messages.size() - 1);
        });
    }

    private void setStatusThinking() {
        if (tvStatus != null) {
            tvStatus.setText("正在输入...");
            tvStatus.setTextColor(getResources().getColor(R.color.chat_send_btn, getTheme()));
        }
    }

    private void clearThinking() {
        if (tvStatus != null && aiChatClient != null && aiChatClient.isConnected()) {
            tvStatus.setText("在线");
            tvStatus.setTextColor(getResources().getColor(R.color.chat_time_text, getTheme()));
        }
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

            int maxW = (int) (holder.tvMessage.getResources().getDisplayMetrics().density * 260);
            holder.tvMessage.setMaxWidth(maxW);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            if (msg.isUser()) {
                holder.tvMessage.setBackgroundResource(R.drawable.bg_bubble_user);
                holder.tvMessage.setTextColor(holder.itemView.getContext().getColor(R.color.chat_text_user));
                lp.gravity = Gravity.END;
                holder.tvMessage.setElevation(2f);
            } else {
                holder.tvMessage.setBackgroundResource(R.drawable.bg_bubble_ai);
                holder.tvMessage.setTextColor(holder.itemView.getContext().getColor(R.color.chat_text_ai));
                lp.gravity = Gravity.START;
                holder.tvMessage.setElevation(2f);
            }
            holder.itemView.setLayoutParams(lp);
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
}
