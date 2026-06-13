package com.example.bubblepet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private Button btnPermission;
    private Button btnStart;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        btnPermission = findViewById(R.id.btn_permission);
        btnStart = findViewById(R.id.btn_start);

        btnPermission.setOnClickListener(v -> openOverlaySettings());
        btnStart.setOnClickListener(v -> togglePetService());

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
}
