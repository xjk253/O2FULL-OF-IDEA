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
    private Button btnPermission;
    private Button btnStart;
    private EditText etGatewayUrl;
    private Button btnSaveUrl;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnPermission = findViewById(R.id.btn_permission);
        btnStart = findViewById(R.id.btn_start);
        etGatewayUrl = findViewById(R.id.et_gateway_url);
        btnSaveUrl = findViewById(R.id.btn_save_url);

        btnPermission.setOnClickListener(v -> openOverlaySettings());
        btnStart.setOnClickListener(v -> togglePetService());
        btnSaveUrl.setOnClickListener(v -> saveGatewayUrl());

        loadGatewayUrl();
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
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
