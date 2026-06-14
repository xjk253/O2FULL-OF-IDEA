package com.example.bubblepet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class OverlayPetService extends Service {

    private static final String CHANNEL_ID = "bubble_pet_channel";
    private static final int NOTIFICATION_ID = 1;

    private WindowManager windowManager;
    private BubblePetView petView;
    private WindowManager.LayoutParams petParams;
    private ChatBubbleView chatBubbleView;
    private WindowManager.LayoutParams chatBubbleParams;
    private boolean isChatVisible = false;
    private AiChatClient aiChatClient;
    private TtsPlayer ttsPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        aiChatClient = AiChatClient.getInstance(this);
        aiChatClient.connect();
        ttsPlayer = new TtsPlayer(this);
        aiChatClient.addOnSentenceListener(sentence -> {
            if (petView != null && sentence.expression != null) {
                petView.setExpression(sentence.expression);
            }
            if (ttsPlayer != null && sentence.ttsText != null) {
                ttsPlayer.speak(sentence.ttsText);
            }
        });
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        addPetToWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
        }
        return new Notification.Builder(this)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void addPetToWindow() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        petView = new BubblePetView(this);
        petView.setScreenSize(screenWidth, screenHeight);
        petView.setOnPetClickListener(view -> toggleChatBubble());
        petView.setOnPositionChangedListener((x, y) -> {
            petParams.x = x;
            petParams.y = y;
            try {
                windowManager.updateViewLayout(petView, petParams);
            } catch (IllegalArgumentException e) {
                // view not attached
            }
        });

        int petType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            petType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            petType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        int initialX = (int) (Math.random() * Math.max(1, screenWidth - 200));
        int initialY = (int) (Math.random() * Math.max(1, screenHeight / 2));
        petView.setInitialPosition(initialX, initialY);

        petParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                petType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        petParams.gravity = Gravity.TOP | Gravity.START;
        petParams.x = initialX;
        petParams.y = initialY;

        windowManager.addView(petView, petParams);

        petView.post(() -> {
            petView.startBreath();
            petView.scheduleWander();
        });
    }

    private void toggleChatBubble() {
        if (isChatVisible) {
            hideChatBubble();
        } else {
            showChatBubble();
        }
    }

    private void showChatBubble() {
        if (chatBubbleView != null) return;
        chatBubbleView = new ChatBubbleView(this);
        chatBubbleView.setOnChatListener(new ChatBubbleView.OnChatListener() {
            @Override
            public void onSendMessage(String message) {
                aiChatClient.sendMessage(message, reply ->
                        chatBubbleView.setLastMessage(reply)
                );
            }

            @Override
            public void onExpand() {
                hideChatBubble();
                Intent intent = new Intent(OverlayPetService.this, ChatActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onClose() {
                hideChatBubble();
            }
        });

        int chatType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            chatType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            chatType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        chatBubbleParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                chatType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        chatBubbleParams.gravity = Gravity.TOP | Gravity.START;
        chatBubbleParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        float petCenterX = petParams.x + petView.getWidth() / 2f;
        float petTopY = petParams.y;
        int chatWidth = (int) (260 * getResources().getDisplayMetrics().density);
        chatBubbleParams.x = (int) (petCenterX - chatWidth / 2f);
        chatBubbleParams.y = (int) petTopY - 300;
        if (chatBubbleParams.y < 0) {
            chatBubbleParams.y = (int) (petTopY + petView.getHeight());
        }

        windowManager.addView(chatBubbleView, chatBubbleParams);
        isChatVisible = true;
    }

    private void hideChatBubble() {
        if (chatBubbleView != null) {
            windowManager.removeView(chatBubbleView);
            chatBubbleView = null;
        }
        isChatVisible = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (petView != null) {
            petView.destroy();
            windowManager.removeView(petView);
            petView = null;
        }
        hideChatBubble();
        // 单例由所有组件共享，服务销毁时不 disconnect，保持连接给 ChatActivity 复用
        if (ttsPlayer != null) {
            ttsPlayer.destroy();
            ttsPlayer = null;
        }
        aiChatClient = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
