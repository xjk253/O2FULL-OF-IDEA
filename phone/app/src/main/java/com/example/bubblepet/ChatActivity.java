package com.example.bubblepet;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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

        aiChatClient = AiChatClient.getInstance(this);
        aiChatClient.connect();

        adapter = new ChatAdapter(messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

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
        // 从 Service 用 FLAG_ACTIVITY_NEW_TASK 启动时，系统不会自动弹键盘
        // 需要主动请求焦点 + 显示输入法
        etChatInput.postDelayed(() -> {
            etChatInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etChatInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    private void sendMessage(String text) {
        messages.add(new ChatMessage(text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);

        aiChatClient.sendMessage(text, reply -> {
            messages.add(new ChatMessage(reply, false));
            adapter.notifyItemInserted(messages.size() - 1);
            rvChat.scrollToPosition(messages.size() - 1);
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 单例由所有组件共享，不在这里销毁
        aiChatClient = null;
    }
}
