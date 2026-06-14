package com.example.bubblepet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "bubblepet";
    private static final String KEY_GATEWAY_URL = "gateway_url";

    private TextView tvStatus;
    private TextView tvConnectionStatus;
    private TextView tvUsageAccessDesc;
    private Button btnPermission;
    private Button btnStart;
    private Button btnUsageAccess;
    private EditText etGatewayUrl;
    private Button btnSaveUrl;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        tvUsageAccessDesc = findViewById(R.id.tv_usage_access_desc);
        btnPermission = findViewById(R.id.btn_permission);
        btnStart = findViewById(R.id.btn_start);
        btnUsageAccess = findViewById(R.id.btn_usage_access);
        etGatewayUrl = findViewById(R.id.et_gateway_url);
        btnSaveUrl = findViewById(R.id.btn_save_url);

        btnPermission.setOnClickListener(v -> openOverlaySettings());
        btnStart.setOnClickListener(v -> togglePetService());
        btnSaveUrl.setOnClickListener(v -> saveGatewayUrl());
        btnUsageAccess.setOnClickListener(v ->
                ForegroundAppDetector.openUsageAccessSettings(this));

        loadGatewayUrl();
        checkPermissions();
        setupConnectionStatus();
        checkUsageAccess();
    }

    private void setupConnectionStatus() {
        // 权限就绪后才显示连接状态
        if (!hasOverlayPermission()) return;

        tvConnectionStatus.setVisibility(View.VISIBLE);
        AiChatClient client = AiChatClient.getInstance(this);
        client.setOnConnectionListener(connected -> {
            if (connected) {
                tvConnectionStatus.setText(R.string.connection_connected);
                tvConnectionStatus.setTextColor(0xFF2E7D32);
            } else {
                tvConnectionStatus.setText(R.string.connection_disconnected);
                tvConnectionStatus.setTextColor(0xFFC62828);
            }
        });
        // 已连接直接刷新一次,否则触发连接
        if (client.isConnected()) {
            tvConnectionStatus.setText(R.string.connection_connected);
            tvConnectionStatus.setTextColor(0xFF2E7D32);
        } else {
            client.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        setupConnectionStatus();
        checkUsageAccess();
    }

    private void checkPermissions() {
        if (hasOverlayPermission()) {
            tvStatus.setText(R.string.permission_granted);
            btnPermission.setVisibility(View.GONE);
            btnStart.setVisibility(View.VISIBLE);
            updateStartButton();
        } else {
            tvStatus.setText(R.string.permission_denied_overlay);
            btnPermission.setVisibility(View.VISIBLE);
            btnStart.setVisibility(View.GONE);
        }
    }

    private boolean hasOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void togglePetService() {
        Intent intent = new Intent(this, OverlayPetService.class);
        if (isServiceRunning) {
            stopService(intent);
            isServiceRunning = false;
        } else {
            startForegroundService(intent);
            isServiceRunning = true;
        }
        updateStartButton();
    }

    private void updateStartButton() {
        if (isServiceRunning) {
            btnStart.setText(R.string.btn_stop_pet);
        } else {
            btnStart.setText(R.string.btn_start_pet);
        }
    }

    private void checkUsageAccess() {
        ForegroundAppDetector detector = new ForegroundAppDetector(this);
        if (detector.hasPermission()) {
            btnUsageAccess.setVisibility(View.GONE);
            tvUsageAccessDesc.setText(R.string.usage_access_granted);
            tvUsageAccessDesc.setTextColor(0xFF2E7D32);
        } else {
            btnUsageAccess.setVisibility(View.VISIBLE);
        }
    }

    private void loadGatewayUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(KEY_GATEWAY_URL, null);
        etGatewayUrl.setText(saved != null ? saved : getString(R.string.gateway_url));
    }

    private void saveGatewayUrl() {
        String url = etGatewayUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.gateway_url_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_GATEWAY_URL, url)
                .apply();
        Toast.makeText(this, R.string.gateway_url_saved, Toast.LENGTH_SHORT).show();
    }
}
