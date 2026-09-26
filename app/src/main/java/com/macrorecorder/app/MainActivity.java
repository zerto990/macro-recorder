package com.macrorecorder.app;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private MacroRecorderService service;
    private boolean bound = false;
    private TextView tvStatus, tvRoot, tvEvents;
    private Button btnRecord, btnStop, btnPlay, btnService;

    private final ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName n, IBinder b) {
            service = ((MacroRecorderService.LocalBinder) b).getService();
            bound = true;
            tvStatus.setText("✅ Сервис запущен");
            new Thread(() -> {
                boolean r = service.getRoot().checkRoot();
                runOnUiThread(() -> tvRoot.setText(r ? "✅ Root есть" : "❌ Нет Root"));
            }).start();
        }
        public void onServiceDisconnected(ComponentName n) { bound = false; tvStatus.setText("⚪ Сервис остановлен"); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверка overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        }

        // Создаём UI программно (без XML layout для простоты)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xFF121212);

        TextView title = new TextView(this);
        title.setText("🎯 Macro Recorder");
        title.setTextSize(28);
        title.setTextColor(0xFFFFFFFF);
        layout.addView(title);

        tvRoot = new TextView(this);
        tvRoot.setText("⏳ Проверка Root...");
        tvRoot.setTextSize(16);
        tvRoot.setTextColor(0xFFB0B0B0);
        layout.addView(tvRoot);

        tvStatus = new TextView(this);
        tvStatus.setText("⚪ Сервис остановлен");
        tvStatus.setTextSize(16);
        tvStatus.setTextColor(0xFFB0B0B0);
        layout.addView(tvStatus);

        tvEvents = new TextView(this);
        tvEvents.setText("Записано действий: 0");
        tvEvents.setTextSize(16);
        tvEvents.setTextColor(0xFFFF9800);
        layout.addView(tvEvents);

        btnService = new Button(this);
        btnService.setText("🚀 Запустить сервис");
        btnService.setOnClickListener(v -> startService_());
        layout.addView(btnService);

        btnRecord = new Button(this);
        btnRecord.setText("🔴 Начать запись");
        btnRecord.setOnClickListener(v -> {
            if (bound && service != null) {
                service.startRecording();
                tvEvents.setText("🔴 Запись...");
            }
        });
        layout.addView(btnRecord);

        btnStop = new Button(this);
        btnStop.setText("⏹ Остановить запись");
        btnStop.setOnClickListener(v -> {
            if (bound && service != null && service.isRecording()) {
                MacroScript s = service.stopRecording();
                tvEvents.setText("✅ Записано: " + (s != null ? s.getEventCount() : 0) + " действий");
            }
        });
        layout.addView(btnStop);

        btnPlay = new Button(this);
        btnPlay.setText("▶ Воспроизвести последний");
        btnPlay.setOnClickListener(v -> {
            if (bound && service != null) {
                java.util.List<MacroScript> scripts = service.getStorage().loadAll();
                if (!scripts.isEmpty()) {
                    service.playScript(scripts.get(scripts.size() - 1));
                    tvEvents.setText("▶ Воспроизведение...");
                } else {
                    tvEvents.setText("Нет скриптов для воспроизведения");
                }
            }
        });
        layout.addView(btnPlay);

        TextView info = new TextView(this);
        info.setText("\n📌 Инструкция:\n1. Запустите сервис\n2. Нажмите «Начать запись»\n3. Сворачивайте приложение и делайте тапы по экрану\n4. Вернитесь и нажмите «Остановить»\n5. Нажмите «Воспроизвести»\n\n⚠️ Нужен ROOT для работы!");
        info.setTextSize(13);
        info.setTextColor(0xFF888888);
        layout.addView(info);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void startService_() {
        Intent i = new Intent(this, MacroRecorderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
        else startService(i);
        bindService(i, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) unbindService(conn);
    }
}
