package com.example.bubblepet;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatBubbleView extends LinearLayout {

    private TextView tvLastMessage;
    private EditText etChatInput;
    private Button btnSend;
    private Button btnExpand;
    private Button btnCloseChat;

    public interface OnChatListener {
        void onSendMessage(String message);
        void onExpand();
        void onClose();
    }

    private OnChatListener listener;

    public ChatBubbleView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_chat_bubble, this, true);
        tvLastMessage = findViewById(R.id.tv_last_message);
        etChatInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_send);
        btnExpand = findViewById(R.id.btn_expand);
        btnCloseChat = findViewById(R.id.btn_close_chat);

        btnSend.setOnClickListener(v -> {
            String msg = etChatInput.getText().toString().trim();
            if (!TextUtils.isEmpty(msg) && listener != null) {
                listener.onSendMessage(msg);
                etChatInput.setText("");
            }
        });

        btnExpand.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpand();
            }
        });

        btnCloseChat.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClose();
            }
        });

        etChatInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }
            return false;
        });

        etChatInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) context
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    public void setLastMessage(String message) {
        tvLastMessage.setText(message);
        tvLastMessage.setVisibility(VISIBLE);
    }

    public void setOnChatListener(OnChatListener listener) {
        this.listener = listener;
    }
}
