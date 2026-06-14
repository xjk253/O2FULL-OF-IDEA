package com.example.bubblepet;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TtsPlayer {

    private static final String TAG = "BubblePet";

    private volatile TextToSpeech tts;
    private volatile boolean ready = false;
    private volatile boolean enabled = true;

    public TtsPlayer(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS 中文不可用,回退默认");
                    tts.setLanguage(Locale.getDefault());
                }
                ready = true;
                Log.d(TAG, "TTS 初始化完成");
            } else {
                Log.e(TAG, "TTS 初始化失败 status=" + status);
            }
        });
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            stop();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void speak(String text) {
        if (!enabled || !ready || tts == null || text == null || text.isEmpty()) {
            return;
        }
        // 加入队列,这样多句连续朗读会按顺序播放,而不是被后一句打断
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "bubble_sentence_" + System.nanoTime());
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }
}
